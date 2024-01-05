package shx.kitsune.scripting;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.bukkit.plugin.Plugin;

import com.terheyden.str.Str;

import shx.kitsune.Kitsune;

public class Script {
    private final Class<?> compiledClass;
    private final CharSequence code;
    private final File file;
    private Object instance;
    private boolean loaded = false;
    private boolean unloaded = false;

    public Script(Class<?> compiledClass, File file, CharSequence code) {
        this.compiledClass = compiledClass;
        this.file = file;
        this.code = code;
    }

    public File getFile() {
        return file;
    }

    public CharSequence getCode() {
        return code;
    }

    public Class<?> getCompiledClass() {
        return compiledClass;
    }

    public void load() {
        if ( loaded && !unloaded ) return;

        try {
            Method load = compiledClass.getDeclaredMethod("load");
            load.setAccessible(true);
            load.invoke(instance);
            loaded = true;
            unloaded = false;
        } catch (Exception e) {
            Kitsune.getPlugin().getLogger().warning(
                Str.format("Script <{}> failed to load. The script does not contain a load() method.", getCompiledClass())
            );
        }
    }

    public void unload() {
        if ( unloaded && !loaded ) return;
        
        try {
            Method unload = compiledClass.getDeclaredMethod("unload");
            unload.setAccessible(true);
            unload.invoke(instance);
            unloaded = true;
            loaded = false;
        } catch (Exception e) {
            Kitsune.getPlugin().getLogger().warning(
                Str.format("Script <{}> failed to unload. The script does not contain an unload() method.", getCompiledClass())
            );
        }
    }

    public Object getInstance() {
        if ( instance == null ) {
            try {
                try {
                    Constructor<?> constructor = compiledClass.getConstructor(Plugin.class);
                    constructor.setAccessible(true);
                    instance = constructor.newInstance(Kitsune.getPlugin());
                } catch (NoSuchMethodException e) {
                    Kitsune.getPlugin().getLogger().warning(
                        Str.format("Script <{}> does not have a Plugin constructor. Use the default constructor instead.", getCompiledClass())
                    );

                    Constructor<?> constructor = compiledClass.getConstructor();
                    constructor.setAccessible(true);
                    instance = constructor.newInstance();
                }

                load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return instance;
    }
}
