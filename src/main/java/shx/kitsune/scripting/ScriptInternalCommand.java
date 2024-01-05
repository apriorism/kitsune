package shx.kitsune.scripting;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ScriptInternalCommand implements CommandExecutor {
    private final ScriptManager manager;

    private List<String> commandLists = new ArrayList<>();

    public ScriptInternalCommand(ScriptManager manager) {
        this.manager = manager;

        commandLists.add("reload");
        commandLists.add("help");
    }

    private void reload(CommandSender sender, String[] args) {
        String script = args.length > 1 ? args[1] : null;

        try {
            if ( manager.reloadScript(script) ) {
                sender.sendMessage("Reloaded " + script);
            } else {
                sender.sendMessage("Failed to reload " + script);
            }
        } catch (ScriptException e) {
            sender.sendMessage("Failed to reload " + script + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void help(CommandSender sender, String[] args) {
        List<String> msg = new ArrayList<>();

        msg.add("Available commands:\n");
        commandLists.forEach(msg::add);

        String message = msg.stream().collect(Collectors.joining(", "));
        sender.sendMessage(message);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0] : "";

        if ( commandLists.contains(sub) ) {
            switch (sub) {
                case "reload":
                    reload(sender, args);
                    break;
                default:
                    help(sender, args);
                    break;
            }
        } else {
            help(sender, args);
        }

        return true;
    }
}
