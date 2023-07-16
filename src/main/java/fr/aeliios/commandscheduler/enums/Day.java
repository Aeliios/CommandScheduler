package fr.aeliios.commandscheduler.enums;

import java.time.DayOfWeek;
import java.util.Optional;

public enum Day {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    FIRST_DAY,
    LAST_DAY;

    /**
     * Convert the enum to an {@link Optional} of {@link DayOfWeek}
     * <p>
     * If the enum is {@link Day#FIRST_DAY} or {@link Day#LAST_DAY}, returns {@link Optional#empty()}
     * </p>
     */
    public Optional<DayOfWeek> toDayOfWeek() {
        if (this != Day.FIRST_DAY && this != Day.LAST_DAY) {
            return Optional.of(DayOfWeek.valueOf(this.name()));
        } else {
            return Optional.empty();
        }
    }
}
