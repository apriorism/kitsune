package shx.kitsune;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import shx.kitsune.scripting.ScriptManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class Kitsune extends JavaPlugin implements Listener {
    private static Plugin plugin;
    private Database database;
    private ScriptManager scriptManager;
    private StateStore stateStore;

    @Override
    public void onEnable() {
        plugin = this;
        
        Configuration.initDefaults();
        Dependencies.checkDependencies();

        getServer().getPluginManager().registerEvents(this, this);

        database = new Database();
        stateStore = new StateStore();
        scriptManager = new ScriptManager();
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

    public Database getDatabase() {
        return database;
    }

    public StateStore getStateStore() {
        return stateStore;
    }
}