package shx.kitsune.scripting;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import shx.kitsune.Kitsune;
import shx.kitsune.utils.FileIO;

import javax.script.ScriptException;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScriptManager {
    private final Kitsune kitsune;
    private final HashMap<Class<?>, Listener> scriptListeners;
    private final HashMap<Class<?>, ScriptCommandWrapper> scriptCommands;
    private final ScriptEngine engine;
    private SimpleCommandMap reflectedCommandMap;
    private Map<String, Command> knownCommands;

    public ScriptManager(Kitsune kitsune) {
        this.kitsune = kitsune;
        this.engine = new ScriptEngine();

        scriptListeners = new HashMap<>();
        scriptCommands = new HashMap<>();
    }

    public boolean loadScript(File script) {
        String scriptName = script.getName().substring(0, script.getName().length() - ".java".length());
        String contents = FileIO.readData(script);
        try {
            kitsune.getLogger().info("Compiling " + scriptName + "...");
            Script Script = engine.compile(script, contents);

            Object instance = Script.getInstance();
            if (instance instanceof Listener) {
                registerListener(Script);
            }

            if (instance instanceof CommandExecutor) {
                registerCommand(Script);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadScript(String name) throws ScriptException {
        Script script = engine.get(name);
        if (script != null) return false;

        File newScript = new File(Kitsune.getScriptFolder(), name + ".java");

        if (!newScript.exists()) return false;
        return loadScript(newScript);
    }

    public boolean unloadScript(String name) {
        Script script = engine.get(name);
        if (script == null) return false;
        unloadScript(script);
        engine.removeScript(name);
        return true;
    }

    public boolean reloadScript(String name) throws ScriptException{
        unloadScript(name);
        return loadScript(name);
    }

    public void unloadScript(Script script) {
        try {
            Object instance = script.getInstance();
            if (instance instanceof Listener) {
                unregisterListener(script);
            }

            if (instance instanceof CommandExecutor) {
                unregisterCommand(script);
            }

            script.unload();

            engine.removeScript(script);
        } catch (Exception e) {
            Kitsune.getPlugin().getLogger().warning("Script " + script.getCompiledClass() + " does not have an unload method!");
        }
    }

    public void load() {
        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            reflectedCommandMap = (SimpleCommandMap) cmdMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bukkit.getServer().getPluginCommand("kitsune").setExecutor(new ScriptInternalCommand(this));

        File[] fileScripts = Kitsune.getScriptFolder().listFiles((f, n) -> n.toLowerCase().endsWith(".java"));
        if (fileScripts == null || fileScripts.length == 0) {
            kitsune.getLogger().warning("No scripts found in " + Kitsune.getScriptFolder());
            return;
        }

        try {
            List<Script> Scripts = engine.compileAll(Arrays.asList(fileScripts));

            for (Script Script : Scripts) {
                Object instance = Script.getInstance();
                if (instance instanceof Listener) {
                    registerListener(Script);

                }
                if (instance instanceof CommandExecutor) {
                    registerCommand(Script);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public void unload() {
        for (Listener listener : scriptListeners.values()) {
            unregisterListener(listener);
        }
        scriptListeners.clear();

        for (ScriptCommandWrapper cmd : scriptCommands.values()) {
            unregisterCommand(cmd);
        }
        scriptCommands.clear();

        engine.removeAll();
        Bukkit.getServer().getPluginCommand("kitsune").setExecutor(null);
    }

    private void registerListener(Script Script) {
        Bukkit.getPluginManager().registerEvents((Listener) Script.getInstance(), kitsune);
        scriptListeners.put(Script.getCompiledClass(), kitsune);
    }

    private void unregisterListener(Script Script) {
        HandlerList.unregisterAll((Listener) Script.getInstance());
        scriptListeners.remove(Script.getCompiledClass());
    }

    private void unregisterListener(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    private void unregisterCommand(Script Script) {
        ScriptCommandWrapper cmd = scriptCommands.get(Script.getCompiledClass());
        cmd.setExecutor(null);
        unregisterCommand(cmd);
        scriptCommands.remove(Script.getCompiledClass());
    }

    private void unregisterCommand(ScriptCommandWrapper cmd) {
        if (reflectedCommandMap == null) return;
        try {
            try {
                cmd.unregister(reflectedCommandMap);
            } catch (Exception ignored) {}

            if (knownCommands == null) {
                try {
                    Field knownCmdsField = reflectedCommandMap.getClass().getDeclaredField("knownCommands");
                    knownCmdsField.setAccessible(true);
                    knownCommands = (Map<String, Command>) knownCmdsField.get(reflectedCommandMap);
                } catch (Exception ignored1) {}

                if (knownCommands == null) {
                    try {
                        Field knownCmdsField = reflectedCommandMap.getClass().getField("knownCommands");
                        knownCmdsField.setAccessible(true);
                        knownCommands = (Map<String, Command>) knownCmdsField.get(reflectedCommandMap);
                    } catch (Exception ignored2) {}
                }

                if (knownCommands == null) {
                    try {
                        Method knownCmdsMethod = reflectedCommandMap.getClass().getDeclaredMethod("getKnownCommands");
                        knownCmdsMethod.setAccessible(true);
                        knownCommands = (Map<String, Command>) knownCmdsMethod.invoke(reflectedCommandMap);
                    } catch (Exception ignored3) {}
                }

                if (knownCommands == null) {
                    try {
                        Method knownCmdsMethod = reflectedCommandMap.getClass().getMethod("getKnownCommands");
                        knownCmdsMethod.setAccessible(true);
                        knownCommands = (Map<String, Command>) knownCmdsMethod.invoke(reflectedCommandMap);
                    } catch (Exception ignored4) {}
                }
            }

            // At this point, the knownCommand map should not be null.
            knownCommands.remove(cmd.getName());
            knownCommands.remove("kitsune:" + cmd.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerCommand(Script instance) {
        if (reflectedCommandMap == null) return;
        try {
            Method getCommand = instance.getCompiledClass().getDeclaredMethod("getCommand");
            getCommand.setAccessible(true);
            if (getCommand.getReturnType() != String.class) throw new ScriptException("getCommand does not return a String");
            String name = (String) getCommand.invoke(instance.getInstance());
            ScriptCommandWrapper cmd = new ScriptCommandWrapper(name, (CommandExecutor) instance.getInstance());
            reflectedCommandMap.register("kitsune", cmd);
            scriptCommands.put(instance.getCompiledClass(), cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        engine.removeAll();
        
        File[] fileScripts = Kitsune.getScriptFolder().listFiles((f, n) -> n.endsWith(".java"));
        if (fileScripts == null) return;
        
        try {
            List<Script> Scripts = engine.compileAll(Arrays.asList(fileScripts));
            for (Script Script : Scripts) {
                Object instance = Script.getInstance();
                if (instance instanceof Listener) {
                    registerListener(Script);
                }

                if (instance instanceof CommandExecutor) {
                    registerCommand(Script);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
