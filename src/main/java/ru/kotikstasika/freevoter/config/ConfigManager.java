package ru.kotikstasika.freevoter.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.configuration.file.FileConfiguration;
import ru.kotikstasika.freevoter.FreeVoter;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfigManager {
    final FreeVoter plugin;
    String token;
    int serverId;
    boolean debug;

    List<String> rewardMessages;
    List<String> rewardCommands;

    List<String> hasKassedMessage;
    List<String> noVotesMessage;
    List<String> oldVotesMessage;
    List<String> giveMessage;
    List<String> giveAllMessage;

    String errorDefault;
    String error401Console;
    String error403Console;
    String error404Console;
    String error429Console;
    String fixRaceCondishinal;

    String dbHost;
    int dbPort;
    String dbName;
    String dbUser;
    String dbPassword;

    public ConfigManager(FreeVoter plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.token = config.getString("settings.token", "");
        this.serverId = config.getInt("settings.server-id", 0);
        this.debug = config.getBoolean("settings.debug", false);

        this.rewardMessages = config.getStringList("reward.message");
        this.rewardCommands = config.getStringList("reward.command");

        this.hasKassedMessage = config.getStringList("message.haskd");
        this.noVotesMessage = config.getStringList("message.nonegolos");
        this.giveMessage = config.getStringList("message.give");
        this.giveAllMessage = config.getStringList("message.alertall");
        this.oldVotesMessage = config.getStringList("message.old");

        this.errorDefault = config.getString("message.errors.default", "");
        this.error401Console = config.getString("message.errors.401.console", "");
        this.error403Console = config.getString("message.errors.403.console", "");
        this.error404Console = config.getString("message.errors.404.console", "");
        this.error429Console = config.getString("message.errors.429.console", "");
        this.fixRaceCondishinal = config.getString("message.fixRaceCondishinal", "");

        this.dbHost = config.getString("mysql.host", "localhost");
        this.dbPort = config.getInt("mysql.port", 3306);
        this.dbName = config.getString("mysql.database", "freevoter");
        this.dbUser = config.getString("mysql.username", "root");
        this.dbPassword = config.getString("mysql.password", "");
    }
}
