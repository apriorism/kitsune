package shx.kitsune;

import org.bukkit.configuration.file.FileConfiguration;

public class Configuration {
    public static void initDefaults() {
        FileConfiguration config = Kitsune.getPlugin().getConfig();

        config.addDefault("config_version", "1.0");

        // mysql databases
        config.addDefault("db.host", "");
        config.addDefault("db.port", 3306);
        config.addDefault("db.database", "");
        config.addDefault("db.username", "");
        config.addDefault("db.password", "");
        config.addDefault("db.max_pool_size", 10);
        config.addDefault("db.connection_timeout", 30000);
        config.addDefault("db.idle_timeout", 60000);
        config.addDefault("db.leak_detection_threshold", 30000);

        config.options().copyDefaults(true);

        Kitsune.getPlugin().saveConfig();
    }
}
