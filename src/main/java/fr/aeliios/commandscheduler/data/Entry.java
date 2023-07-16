package fr.aeliios.commandscheduler.data;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class Entry {
    private final String name;
    private final LocalTime time;
    private final TimeScope timeScope;
    private final List<String> commands;
    private @Nullable LocalDateTime lastExecution;

    public Entry(String name, LocalTime time, TimeScope timeScope, List<String> commands) {
        this.name = name;
        this.time = time;
        this.timeScope = timeScope;
        this.commands = commands;
    }

    public String getName() {
        return this.name;
    }

    public LocalTime getTime() {
        return this.time;
    }

    public TimeScope getTimeScope() {
        return this.timeScope;
    }

    public List<String> getCommands() {
        return this.commands;
    }

    public @Nullable LocalDateTime getLastExecution() {
        return this.lastExecution;
    }

    public void setLastExecution(LocalDateTime lastExecution) {
        this.lastExecution = lastExecution;
    }
}
