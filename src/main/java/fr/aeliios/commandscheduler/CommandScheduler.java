package fr.aeliios.commandscheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.aeliios.commandscheduler.adapters.LocalDateTimeAdapter;
import fr.aeliios.commandscheduler.adapters.LocalTimeAdapter;
import fr.aeliios.commandscheduler.adapters.TimeScopeAdapter;
import fr.aeliios.commandscheduler.data.Entry;
import fr.aeliios.commandscheduler.enums.TimeScope;
import fr.aeliios.commandscheduler.expansion.CommandSchedulerExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class CommandScheduler extends JavaPlugin {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
            .registerTypeAdapter(TimeScope.class, new TimeScopeAdapter())
            .create();

    private final File commandsFile = new File(this.getDataFolder(), "commands.json");

    private final Map<String, LocalDateTime> tasks = new HashMap<>();

    public Map<String, LocalDateTime> getTasks() {
        return this.tasks;
    }

    @Override
    public void onEnable() {
        new CommandSchedulerExpansion(this).register();

        this.createPluginDirectory();

        File commandsExample = new File(this.getDataFolder(), "commands_example.json");
        if (!commandsExample.exists()) {
            Entry entry1 = new Entry("entry1", LocalTime.of(12, 42, 42), TimeScope.DAILY, List.of("command1", "command2"));
            Entry entry2 = new Entry("entry2", LocalTime.of(12, 42), TimeScope.WEEKLY, List.of("command3", "command4"));
            Entry entry3 = new Entry("entry3", LocalTime.of(12, 42, 42), TimeScope.MONTHLY, List.of("command5", "command6"));
            entry3.setLastExecution(LocalDateTime.now());
            Entry entry4 = new Entry("entry4", LocalTime.now(), TimeScope.YEARLY, List.of("command7", "command8"));
            this.save(new Entry[]{entry1, entry2, entry3, entry4}, commandsExample);
        }

        if (!this.commandsFile.exists()) {
            // Create default commands.json if it doesn't exist by saving empty array
            this.save(new Entry[]{}, this.commandsFile);
            return;
        }

        Entry[] entries = this.load(this.commandsFile);

        if (entries == null) {
            return;
        }

        for (Entry entry : entries) {
            LocalDateTime lastExecution = entry.getLastExecution();
            if (lastExecution == null) {
                lastExecution = LocalDateTime.of(LocalDate.now(), entry.getTime()).minus(1, switch (entry.getTimeScope()) {
                    case DAILY -> ChronoUnit.DAYS;
                    case WEEKLY -> ChronoUnit.WEEKS;
                    case MONTHLY -> ChronoUnit.MONTHS;
                    case YEARLY -> ChronoUnit.YEARS;
                });
            }

            if (!lastExecution.toLocalTime().equals(entry.getTime())) {
                lastExecution = LocalDateTime.of(lastExecution.toLocalDate(), entry.getTime());
            }

            LocalDateTime toExecute = this.getExecuteTime(entry.getTimeScope(), lastExecution);
            ZonedDateTime toExecuteZoned = toExecute.atZone(ZoneId.systemDefault());

            if (this.getNowZoned().isAfter(toExecuteZoned)) {
                this.dispatchCommands(entry);

                entry.setLastExecution(this.getNowZoned().toLocalDateTime());
                this.saveLastExecution(entry, this.commandsFile);

                this.tasks.put(entry.getName(), this.getExecuteTime(entry.getTimeScope(), this.getNowZoned().toLocalDateTime()));

                this.getServer().getLogger().log(Level.INFO, () -> "Executed lately " + entry.getName() + " at " + this.getNowZoned());
                continue;
            }

            if (toExecuteZoned.getDayOfYear() == this.getNowZoned().getDayOfYear()) {
                this.getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                    this.dispatchCommands(entry);

                    entry.setLastExecution(this.getNowZoned().toLocalDateTime());
                    this.saveLastExecution(entry, this.commandsFile);

                    this.tasks.put(entry.getName(), this.getExecuteTime(entry.getTimeScope(), this.getNowZoned().toLocalDateTime()));

                    this.getServer().getLogger().log(Level.INFO, () -> "Executed " + entry.getName() + " at " + this.getNowZoned());
                }, (toExecuteZoned.toLocalTime().toSecondOfDay() - this.getNowZoned().toLocalTime().toSecondOfDay()) * 20L);

                this.getServer().getLogger().log(Level.INFO, () -> "Scheduled " + entry.getName() + " at " + toExecuteZoned);
            }
            this.tasks.put(entry.getName(), toExecuteZoned.toLocalDateTime());
        }
    }

    public void createPluginDirectory() {
        if (!this.getDataFolder().exists()) {
            if (!this.getDataFolder().mkdir()) {
                this.getLogger().log(Level.SEVERE, "Could not create plugin directory");
            }
        }
    }

    public ZonedDateTime getNowZoned() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault());
    }

    public LocalDateTime getExecuteTime(TimeScope timeScope, LocalDateTime localDateTime) {
        return switch (timeScope) {
            case DAILY -> localDateTime.plusDays(1);
            case WEEKLY -> localDateTime.plusWeeks(1);
            case MONTHLY -> localDateTime.plusMonths(1);
            case YEARLY -> localDateTime.plusYears(1);
        };
    }

    public Entry[] load(File file) {
        try (FileReader reader = new FileReader(file)) {
            return this.gson.fromJson(reader, Entry[].class);
        } catch (Exception ex) {
            this.getLogger().log(Level.SEVERE, ex, () -> "Could not load " + file.getName());
            return null;
        }
    }

    public void saveLastExecution(Entry entry, File file) {
        Entry[] fileEntries = this.load(file);

        if (fileEntries == null) {
            return;
        }

        for (Entry fileEntry : fileEntries) {
            if (fileEntry.getName().equals(entry.getName())) {
                fileEntry.setLastExecution(entry.getLastExecution());
                break;
            }
        }

        this.save(fileEntries, file);
    }

    public void save(Entry[] entries, File file) {
        try (PrintWriter writer = new PrintWriter(file)) {
            this.gson.toJson(entries, writer);
            writer.flush();
        } catch (Exception ex) {
            this.getLogger().log(Level.SEVERE, ex, () -> "Could not save " + file.getName());
        }
    }

    public void dispatchCommands(Entry entry) {
        this.getServer().getScheduler().runTask(this, () -> {
            entry.getCommands().forEach(command -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), this.getString(command));
            });
        });
    }

    public String getString(String command) {
        if (command.startsWith("/")) {
            command = command.replaceFirst("/", "");
        }
        String[] split = command.split("%");
        if (split.length == 1) {
            return command;
        } else {
            return PlaceholderAPI.setPlaceholders(null, command);
        }
    }
}
