package fr.aeliios.commandscheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.aeliios.commandscheduler.adapters.LocalDateTimeAdapter;
import fr.aeliios.commandscheduler.adapters.LocalTimeAdapter;
import fr.aeliios.commandscheduler.adapters.TimeScopeAdapter;
import fr.aeliios.commandscheduler.data.Entry;
import fr.aeliios.commandscheduler.data.TimeScope;
import fr.aeliios.commandscheduler.enums.Day;
import fr.aeliios.commandscheduler.enums.Periodicity;
import fr.aeliios.commandscheduler.expansion.CommandSchedulerExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    public Map<String, LocalDateTime> getTasks() {
        return this.tasks;
    }

    @Override
    public void onEnable() {
        new CommandSchedulerExpansion(this).register();

        this.createPluginDirectory();

        File commandsExample = new File(this.getDataFolder(), "commands_example.json");
        if (!commandsExample.exists()) {
            Entry entry1 = new Entry("entry1", LocalTime.of(12, 42),
                    new TimeScope(Periodicity.DAILY, null), List.of("command1", "command2"));

            Entry entry2 = new Entry("entry2", LocalTime.of(12, 42),
                    new TimeScope(Periodicity.WEEKLY, Day.WEDNESDAY), List.of("command3", "command4"));

            Entry entry3 = new Entry("entry3", LocalTime.of(12, 42, 42),
                    new TimeScope(Periodicity.MONTHLY, Day.FIRST_DAY), List.of("command5", "command6"));

            Entry entry4 = new Entry("entry4", LocalTime.of(12, 42, 42, 42),
                    new TimeScope(Periodicity.YEARLY, Day.LAST_DAY), List.of("command7", "command8"));

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
            LocalDateTime toExecute = LocalDateTime.of(LocalDate.now(), entry.getTime());

            //Daily Periodicity has no Day
            if (entry.getTimeScope().periodicity() != Periodicity.DAILY) {
                Day day = entry.getTimeScope().day();
                if (day == null) {
                    this.getLogger().log(Level.WARNING, () -> "TimeScope not valid for " + entry.getName() + " ! Skipping it.");
                    continue;
                }

                toExecute = this.getToExecuteTimeScoped(toExecute, day, entry.getTimeScope().periodicity());
                if (toExecute == null) {
                    this.getLogger().log(Level.WARNING, () -> "TimeScope not valid for " + entry.getName() + " ! Skipping it.");
                    continue;
                }
            }

            ZonedDateTime toExecuteZoned = toExecute.atZone(ZoneId.systemDefault());

            LocalDateTime nextExecution = this.getNextExecution(entry.getTimeScope().periodicity(), this.getLastExecution(entry));
            if (nextExecution.toLocalDate().isAfter(this.getNowZoned().toLocalDate())) {
                this.tasks.put(entry.getName(), toExecuteZoned.toLocalDateTime());
                continue;
            }

            if (this.getNowZoned().isAfter(toExecuteZoned)) {
                this.execute(entry, toExecuteZoned.toLocalDate());
                this.getServer().getLogger().log(Level.INFO, () -> "Executed lately " + entry.getName() + " at " + this.getNowZoned().format(this.formatter));
                continue;
            }

            if (toExecuteZoned.getDayOfYear() == this.getNowZoned().getDayOfYear()) {
                this.getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                    this.execute(entry, toExecuteZoned.toLocalDate());
                    this.getServer().getLogger().log(Level.INFO, () -> "Executed " + entry.getName() + " at " + this.getNowZoned().format(this.formatter));

                }, (toExecuteZoned.toLocalTime().toSecondOfDay() - this.getNowZoned().toLocalTime().toSecondOfDay()) * 20L);

                this.getServer().getLogger().log(Level.INFO, () -> "Scheduled " + entry.getName() + " at " + toExecuteZoned.format(this.formatter));
            }

            this.tasks.put(entry.getName(), toExecuteZoned.toLocalDateTime());
        }
    }

    public void execute(Entry entry, LocalDate executeDate) {
        this.dispatchCommands(entry);
        entry.setLastExecution(LocalDateTime.of(executeDate, entry.getTime()));
        this.saveLastExecution(entry, this.commandsFile);
        this.tasks.put(entry.getName(), this.getNextExecution(entry.getTimeScope().periodicity(), this.getNowZoned().toLocalDateTime()));
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

    public LocalDateTime getToExecuteTimeScoped(LocalDateTime localDateTime, Day day, Periodicity periodicity) {
        switch (day) {
            case FIRST_DAY -> {
                return switch (periodicity) {
                    case WEEKLY -> localDateTime.with(DayOfWeek.MONDAY);
                    case MONTHLY -> localDateTime.withDayOfMonth(1);
                    case YEARLY -> localDateTime.withDayOfYear(1);
                    default -> null;
                };
            }
            case LAST_DAY -> {
                return switch (periodicity) {
                    case WEEKLY -> localDateTime.with(DayOfWeek.SUNDAY);
                    case MONTHLY -> localDateTime.withDayOfMonth(localDateTime.toLocalDate().lengthOfMonth());
                    case YEARLY -> localDateTime.withDayOfYear(localDateTime.toLocalDate().lengthOfYear());
                    default -> null;
                };
            }
            default -> {
                return day.toDayOfWeek().map(localDateTime::with).orElse(null);
            }
        }
    }

    public LocalDateTime getNextExecution(Periodicity periodicity, LocalDateTime localDateTime) {
        return switch (periodicity) {
            case DAILY -> localDateTime.plusDays(1);
            case WEEKLY -> localDateTime.plusWeeks(1);
            case MONTHLY -> localDateTime.plusMonths(1);
            case YEARLY -> localDateTime.plusYears(1);
        };
    }

    public LocalDateTime getLastExecution(Entry entry) {
        LocalDateTime lastExecution = entry.getLastExecution();

        if (lastExecution != null) {
            return lastExecution;
        }

        ChronoUnit chronoUnit = switch (entry.getTimeScope().periodicity()) {
            case DAILY -> ChronoUnit.DAYS;
            case WEEKLY -> ChronoUnit.WEEKS;
            case MONTHLY -> ChronoUnit.MONTHS;
            case YEARLY -> ChronoUnit.YEARS;
        };

        return LocalDateTime.of(LocalDate.now(), entry.getTime()).minus(1, chronoUnit);
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
        if (split.length != 1 && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return PlaceholderAPI.setPlaceholders(null, command);
        } else {
            return command;
        }
    }
}
