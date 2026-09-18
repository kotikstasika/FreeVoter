package ru.kotikstasika.freevoter.command.impl_admin;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.kotikstasika.freevoter.FreeVoter;
import ru.kotikstasika.freevoter.command.SubCommand;
import ru.kotikstasika.freevoter.service.api.VoteService;
import ru.kotikstasika.freevoter.utils.Parser;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatsSubCommand extends SubCommand {
    FreeVoter plugin;
    VoteService voteService;

    public StatsSubCommand(FreeVoter plugin, VoteService voteService) {
        super("stats");
        this.plugin = plugin;
        this.voteService = voteService;
    }

    @Override
    public void execute(CommandSender sender, Command command, String[] args) {
        if (!sender.hasPermission("freevoter.admin")) return;

        if (args.length < 2) {
            leaveUsage(sender, "freevoter");
            return;
        }

        String targetNickname = args[2];
        voteService.getPlayerStats(targetNickname).thenAccept(data -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (data == null || data.getNickname().isEmpty()) {
                    sender.sendMessage("Игрок " + targetNickname + " не найден в базе данных");
                    return;
                }

                sender.sendMessage(Parser.color("Статистика игрока " + data.getNickname()));
                sender.sendMessage(Parser.color("Голосов: " + data.getTotalVotes()));
                sender.sendMessage(Parser.color("Наград получено: " + data.getRewardsClaimed()));
                sender.sendMessage(Parser.color("Последнее голосование: " + data.getLastVoteAt()));
                sender.sendMessage(Parser.color("Последняя награда: " + data.getLastRewardAt()));
            });
        });
    }

    @Override
    public List<String> getTabCompletes(int args) {
        if (args == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        return List.of();
    }

    @Override
    public int getRequiredArgs() {
        return 1;
    }

    @Override
    public String getUsage() {
        return "[никнейм]";
    }
}
