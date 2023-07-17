package fr.aeliios.commandscheduler.expansion;

import fr.aeliios.commandscheduler.CommandScheduler;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;

public class CommandSchedulerExpansion extends PlaceholderExpansion {

    private final CommandScheduler plugin;

    public CommandSchedulerExpansion(CommandScheduler plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cs";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Aeliios";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] args = params.split("_");
        if (args.length != 2) {
            return "";
        }

        LocalDateTime localDateTime = this.plugin.getTasks().get(args[0]);
        if (localDateTime == null) {
            return "";
        }

        Duration duration = Duration.between(LocalDateTime.now(), localDateTime);

        return DurationFormatUtils.formatDuration(duration.toMillis(), args[1]);
    }
}
