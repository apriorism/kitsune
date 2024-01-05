package shx.kitsune.scripting;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.script.ScriptException;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.bukkit.Bukkit;

import com.terheyden.str.Str;

import shx.kitsune.Kitsune;
import shx.kitsune.utils.FileIO;

public class ScriptEngine {
    private final JavaCompiler compiler;
    private final Map<String, ScriptClassLoader> loaders = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    private ScriptMemoryManager scriptMemoryManager;
    private DiagnosticCollector<JavaFileObject> diagnostics;
    private List<String> classPath;

    public ScriptEngine() {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No java compiler found");
        }

        diagnostics = new DiagnosticCollector<>();
        
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        scriptMemoryManager = new ScriptMemoryManager(
            fileManager, getClass().getClassLoader()
        );

        reloadClassPath();
    }

    public void reloadClassPath() {
        classPath = new ArrayList<String>();

        classPath.add(".");
        classPath.add(Bukkit.getServer().getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        String userDir = System.getProperty("user.dir");
        Logger logger = Kitsune.getPlugin().getLogger();

        File plugins = new File(userDir, "plugins" + File.separator);
        List<File> deps = FileIO.traverse(plugins, f -> f.getAbsolutePath().endsWith(".jar"));

        for (File dep : deps) {
            classPath.add(dep.getAbsolutePath());
        }

        File libraries = new File(userDir, "libraries" + File.separator);
        List<File> libs = FileIO.traverse(libraries, f -> f.getAbsolutePath().endsWith(".jar"));

        for (File lib : libs) {
            classPath.add(lib.getAbsolutePath());
        }

        // filter unique classpath
        Set<String> classPathSet = new HashSet<String>(classPath);
        classPath = new ArrayList<String>(classPathSet);

        logger.info(Str.format("Classpath added total of {} jars", classPath.size()));
    }

    public Set<String> getNames() {
        return classes.keySet();
    }

    public Script get(String name) {
        Map.Entry<String, ScriptClassLoader> entry = loaders.entrySet().stream().filter(en -> en.getKey().endsWith(name)).findFirst().orElse(null);
        if (entry == null) return null;

        ScriptClassLoader entryScript = entry.getValue();
        return entryScript.script;
    }

    public Script getByClass(Class<?> clazz) {
        return get(clazz.getName());
    }

    public Script removeScript(String name) {
        Map.Entry<String, ScriptClassLoader> entry = loaders.entrySet().stream().filter(en -> en.getKey().endsWith(name)).findFirst().orElse(null);
        if (entry == null) return null;

        loaders.remove(entry.getKey());
        classes.remove(entry.getKey());

        try {
            entry.getValue().close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ScriptClassLoader entryScript = entry.getValue();
        return entryScript.script;
    }

    public void removeScript(Script skript) {
        Map.Entry<String, ScriptClassLoader> entry = loaders.entrySet().stream().filter(en -> en.getValue().script == skript).findFirst().orElse(null);
        if (entry == null) return;

        loaders.remove(entry.getKey());
        classes.remove(entry.getKey());

        try {
            entry.getValue().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (String current : loaders.keySet()) {
                ScriptClassLoader loader = loaders.get(current);
                try {
                    cachedClass = loader.findClass(name);
                } catch (ClassNotFoundException ignored) {
                }
                if (cachedClass != null) {
                    return cachedClass;
                }
            }
        }
        return null;
    }

    Class<?> getClass(String name) {
        return classes.get(name);
    }

    void setClass(final String name, final Class<?> clazz) {
        if (!classes.containsKey(name)) {
            classes.put(name, clazz);
        }
    }

    boolean removeClass(String name) {
        return classes.remove(name) != null;
    }

    public void removeAll() {
        for (ScriptClassLoader loader : loaders.values()) {
            try {
                loader.script.unload();
                loader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        loaders.clear();
        classes.clear();
    }

    public Iterable<String> getCompilerOptions() {
        List<String> options = new ArrayList<>();
        String classString = classPath.stream().collect(
            Collectors.joining(":")
        );

        options.add("-classpath");
        options.add(classString);

        return options;
    }

    public Script compile(File file, String code) throws ScriptException, MalformedURLException {
        if (compiler == null) {
            throw new IllegalStateException("No java compiler found");
        }

        String fullClassName = FileIO.getFullName(file, code);
        
        FileScriptMemoryJavaObject scriptSource = scriptMemoryManager.createSourceFileObject(file, fullClassName, code);
        Collection<FileScriptMemoryJavaObject> otherScripts = loaders.values().stream().filter(l -> !l.getFullClassName().equals(fullClassName)).map(ScriptClassLoader::getSource).collect(Collectors.toList());
        otherScripts.add(scriptSource);

        JavaCompiler.CompilationTask task = compiler.getTask(
            null,
            scriptMemoryManager,
            diagnostics,
            getCompilerOptions(),
            null,
            otherScripts
        );

        if ( !task.call() ) {
            String message = Str.format(
                "<compile> Error while compiling {}: {}",
                file.getPath(),
                diagnostics.getDiagnostics().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"))
            );

            throw new ScriptException(message);
        }

        for ( Map.Entry<String, ScriptClassLoader> entry : loaders.entrySet() ) {
            ScriptClassLoader prev = entry.getValue();
            removeClass(prev.getFullClassName());

            ScriptClassLoader newLoader = scriptMemoryManager.getClassLoader(
                this,
                prev.getFile(),
                prev.getFullClassName(),
                prev.getSource()
            );

            entry.setValue(newLoader);
            setClass(prev.getFullClassName(), newLoader.script.getCompiledClass());

            try {
                prev.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ScriptClassLoader prev = loaders.remove(fullClassName);
        ScriptClassLoader loader = scriptMemoryManager.getClassLoader(
            this,
            file,
            fullClassName,
            scriptSource
        );

        if ( prev != null ) {
            removeClass(prev.getFullClassName());

            try {
                prev.close();
                prev.clearAssertionStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        loaders.put(fullClassName, loader);
        setClass(fullClassName, loader.script.getCompiledClass());

        return loader.script;
    }

    public Script compile(File file) throws ScriptException, MalformedURLException {
        return compile(file, FileIO.readData(file));
    }

    public List<Script> compileAll(List<File> files) throws ScriptException {
        if ( compiler == null ) {
            throw new IllegalStateException("No java compiler found");
        }

        List<FileScriptMemoryJavaObject> sources = new ArrayList<>(files.size());
        for (File file : files) {
            if (!file.exists()) continue;

            String code = FileIO.readData(file);
            String fullClassName = FileIO.getFullName(file, code);

            FileScriptMemoryJavaObject object = scriptMemoryManager.createSourceFileObject(file, fullClassName, code);
            sources.add(object);
        }

        JavaCompiler.CompilationTask task = compiler.getTask(
            null,
            scriptMemoryManager,
            diagnostics,
            getCompilerOptions(),
            null,
            sources
        );

        if ( !task.call() ) {
            String message = Str.format(
                "<compileAll> Error while compiling: {}",
                diagnostics.getDiagnostics().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"))
            );

            throw new ScriptException(message);
        }

        for ( FileScriptMemoryJavaObject source : sources ) {
            ScriptClassLoader previous = loaders.get(source.getName());

            removeClass(source.getName());
            ScriptClassLoader loader = null;

            try {
                loader = scriptMemoryManager.getClassLoader(this, source.getOrigin(), source.getName(), source);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (loader == null) continue;
            removeClass(source.getName());

            if (previous != null) {
                try {
                    previous.close();
                    previous.clearAssertionStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            loaders.put(source.getName(), loader);
            setClass(source.getName(), loader.script.getCompiledClass());
        }

        return loaders.values().stream().map(l -> l.script).collect(Collectors.toList());
    }
}
