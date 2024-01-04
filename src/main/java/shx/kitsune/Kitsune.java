package shx.kitsune;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class Kitsune extends JavaPlugin implements Listener {
    private static Plugin plugin;
    private Database database;

    @Override
    public void onEnable() {
        plugin = this;
        Configuration.initDefaults();

        getServer().getPluginManager().registerEvents(this, this);

        database = new Database();
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
}