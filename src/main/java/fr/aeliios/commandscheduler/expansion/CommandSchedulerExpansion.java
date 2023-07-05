package fr.aeliios.commandscheduler.expansion;

import fr.aeliios.commandscheduler.CommandScheduler;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class CommandSchedulerExpansion extends PlaceholderExpansion {

    private final CommandScheduler plugin;

    private final ZoneOffset zoneOffset;

    public CommandSchedulerExpansion(CommandScheduler plugin) {
        this.plugin = plugin;
        this.zoneOffset = ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now());
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
        if (args.length != 1) {
            return "";
        }

        LocalDateTime localDateTime = this.plugin.getTasks().get(args[0]);
        if (localDateTime == null) {
            return "";
        }

        return String.valueOf(localDateTime.toEpochSecond(this.zoneOffset) - LocalDateTime.now().toEpochSecond(this.zoneOffset));
    }
}
