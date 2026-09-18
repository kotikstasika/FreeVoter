package ru.kotikstasika.freevoter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kotikstasika.freevoter.command.CommandExecutors;
import ru.kotikstasika.freevoter.command.impl_admin.ListSubCommand;
import ru.kotikstasika.freevoter.command.impl_admin.StatsSubCommand;
import ru.kotikstasika.freevoter.command.impl_player.GetPrizeExecutor;
import ru.kotikstasika.freevoter.config.ConfigManager;
import ru.kotikstasika.freevoter.database.api.DatabaseManager;
import ru.kotikstasika.freevoter.database.impl.DatabaseManagerImpl;
import ru.kotikstasika.freevoter.service.api.VoteService;
import ru.kotikstasika.freevoter.service.impl.VoteServiceImpl;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FreeVoter extends JavaPlugin {
    @Getter
    static FreeVoter instance;
    ConfigManager configManager;
    DatabaseManager databaseManager;
    VoteService voteService;

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManagerImpl(this, configManager);
        databaseManager.initialize();
        this.voteService = new VoteServiceImpl(this, configManager, databaseManager);
        setupCommands();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
            databaseManager = null;
        }
    }

    private void setupCommands() {
        GetPrizeExecutor getPrizeExecutor = new GetPrizeExecutor(voteService);
        getCommand("getprize").setExecutor(getPrizeExecutor);
        getCommand("getprize").setTabCompleter(getPrizeExecutor);

        CommandExecutors freeVoterExecutor = new CommandExecutors();
        new StatsSubCommand(this, voteService).register(freeVoterExecutor);
        new ListSubCommand(this, voteService).register(freeVoterExecutor);
        getCommand("freevoter").setExecutor(freeVoterExecutor);
        getCommand("freevoter").setTabCompleter(freeVoterExecutor);
    }
}
