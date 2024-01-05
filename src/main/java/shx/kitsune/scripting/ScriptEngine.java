package shx.kitsune.scripting;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.bukkit.Bukkit;

import com.terheyden.str.Str;

import shx.kitsune.Kitsune;
import shx.kitsune.utils.FileIO;

public class ScriptEngine {
    private final JavaCompiler compiler;
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    
    private DiagnosticCollector<JavaFileObject> diagnostics;
    private List<String> classPath;

    public ScriptEngine() {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No java compiler found");
        }

        diagnostics = new DiagnosticCollector<>();
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

        logger.info(
            Str.format("Classpath added total of {} jars", classPath.size())
        );
    }

    public Set<String> getNames() {
        return classes.keySet();
    }

    // TODO: implement
    // Class<?> getClassByName(final String name) {
        
    // }

    Class<?> getClass(String name) {
        return classes.get(name);
    }
}
