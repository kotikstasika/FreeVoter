package ru.kotikstasika.freevoter.command.impl_admin;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import ru.kotikstasika.freevoter.FreeVoter;
import ru.kotikstasika.freevoter.command.SubCommand;
import ru.kotikstasika.freevoter.data.PlayerVoteData;
import ru.kotikstasika.freevoter.service.api.VoteService;
import ru.kotikstasika.freevoter.utils.Parser;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListSubCommand extends SubCommand {
    FreeVoter plugin;
    VoteService voteService;

    public ListSubCommand(FreeVoter plugin, VoteService voteService) {
        super("list");
        this.plugin = plugin;
        this.voteService = voteService;
    }

    @Override
    public void execute(CommandSender sender, Command command, String[] args) {
        if (!sender.hasPermission("freevoter.admin")) return;

        voteService.getAllPlayerStats().thenAccept(players -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (players.isEmpty()) {
                    sender.sendMessage("В базе данных нет игроков");
                    return;
                }

                sender.sendMessage(Parser.color("Список игроков:"));
                for (PlayerVoteData data : players) {
                    sender.sendMessage(Parser.color(data.getNickname() + " - Голосов: " + data.getTotalVotes() + " - Наград: " + data.getRewardsClaimed()));
                }
            });
        });
    }

    @Override
    public List<String> getTabCompletes(int args) {
        return List.of();
    }

    @Override
    public int getRequiredArgs() {
        return 0;
    }

    @Override
    public String getUsage() {
        return "";
    }
}
