package fr.aeliios.commandscheduler.data;

import fr.aeliios.commandscheduler.enums.Day;
import fr.aeliios.commandscheduler.enums.Periodicity;

public record TimeScope(Periodicity periodicity, Day day) {
    @Override
    public String toString() {
        return this.periodicity.name() + (this.day == null ? "" : " : " + this.day.name());
    }
}
