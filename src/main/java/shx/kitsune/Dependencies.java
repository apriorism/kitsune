package shx.kitsune;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.terheyden.str.Str;

public class Dependencies {
    static void checkDependencies() {
        List<String> plugins = List.of(
            "vault"
        );

        Plugin kitsune = Kitsune.getPlugin();
        PluginManager pluginManager = kitsune.getServer().getPluginManager();
        Logger logger = kitsune.getLogger();
        
        for (String plugin : plugins) {
            if ( !pluginManager.isPluginEnabled(plugin) ) {
                logger.warning(Str.format("Plugin <{}> is not available.", plugin));
            }
        }
    }
}
