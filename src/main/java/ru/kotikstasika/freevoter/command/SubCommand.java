package ru.kotikstasika.freevoter.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class SubCommand {
    String name;

    public abstract void execute(CommandSender sender, Command command, String[] args);

    public abstract List<String> getTabCompletes(int args);

    public abstract int getRequiredArgs();

    public abstract String getUsage();

    public void register(CommandExecutors executor) {
        executor.getSubCommands().put(name, this);
    }

    public void leaveUsage(CommandSender sender, String commandName) {
        sender.sendMessage("Использование: /" + commandName + " " + name + " " + getUsage());
    }
}
