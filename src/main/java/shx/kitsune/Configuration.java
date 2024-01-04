package shx.kitsune;

import org.bukkit.configuration.file.FileConfiguration;

public class Configuration {
    public static void initDefaults() {
        FileConfiguration config = Kitsune.getPlugin().getConfig();

        config.addDefault("config_version", "1.0");
        config.options().copyDefaults(true);

        Kitsune.getPlugin().saveConfig();
    }
}
