package ru.kotikstasika.freevoter.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kotikstasika.freevoter.utils.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class CommandExecutors implements CommandExecutor, TabCompleter {
    HashMap<String, SubCommand> subCommands = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Parser.color("Использование: /" + alias + " [" + String.join("/", subCommands.keySet()) + "]"));
            return true;
        }

        String subCommandKey = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandKey);
        if (subCommand == null) {
            sender.sendMessage(Parser.color("Использование: /" + alias + " [" + String.join("/", subCommands.keySet()) + "]"));
            return true;
        }

        if (subCommand.getRequiredArgs() + 1 > args.length) {
            sender.sendMessage(Parser.color("Использование: /" + alias + " " + subCommandKey + " " + subCommand.getUsage()));
            return true;
        }

        subCommand.execute(sender, command, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(subCommands.keySet());
        } else {
            SubCommand subCommand = subCommands.get(args[0]);
            if (subCommand != null) {
                completions.addAll(subCommand.getTabCompletes(args.length - 1));
            }
        }

        List<String> filtered = new ArrayList<>();
        for (String s : completions) {
            if (s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                filtered.add(s);
            }
        }

        return filtered;
    }
}
