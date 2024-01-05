package shx.kitsune;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import shx.kitsune.scripting.ScriptManager;

import java.io.File;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class Kitsune extends JavaPlugin implements Listener {
    private static Plugin plugin;
    private static File scriptFolder;

    private Database database;
    private StateStore stateStore;
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        plugin = this;
        scriptFolder = new File(getDataFolder(), "scripts");
        
        Configuration.initDefaults();
        Dependencies.checkDependencies();
        Dependencies.checkScriptsFolder();

        database = new Database();
        stateStore = new StateStore();
        
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            getLogger().info("Kitsune loading...");
            scriptManager = new ScriptManager(this);
            scriptManager.load();
        });
    }

    @Override
    public void onDisable() {
        plugin = null;
        HandlerList.unregisterAll((JavaPlugin) this);
    }

    @EventHandler
    public void onPluginLoad(PluginEnableEvent event) {
        getLogger().info("Kitsune loaded successfully");
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    public static File getScriptFolder() {
        return scriptFolder;
    }

    public Database getDatabase() {
        return database;
    }

    public StateStore getStateStore() {
        return stateStore;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }
}