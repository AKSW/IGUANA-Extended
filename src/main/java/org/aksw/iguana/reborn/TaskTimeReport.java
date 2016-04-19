package org.aksw.iguana.reborn;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TaskTimeReport {
    /**
     * The time stamp when the task started
     */
    protected Instant startInstant;

    /**
     * Time until the task stopped - regardless of success or failure
     * Enables easy formatting in any ChronoUnit
     */
    protected Duration duration;

    /**
     * Exception encountered during task execution - if any
     * A task is considered successful, if this is null
     *
     */
    protected Exception exception;

    public TaskTimeReport(Instant startInstant, Duration duration,
            Exception exception) {
        super();
        this.startInstant = startInstant;
        this.duration = duration;
        this.exception = exception;
    }

    public Instant getStartInstant() {
        return startInstant;
    }

    public Duration getDuration() {
        return duration;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((duration == null) ? 0 : duration.hashCode());
        result = prime * result
                + ((exception == null) ? 0 : exception.hashCode());
        result = prime * result
                + ((startInstant == null) ? 0 : startInstant.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaskTimeReport other = (TaskTimeReport) obj;
        if (duration == null) {
            if (other.duration != null)
                return false;
        } else if (!duration.equals(other.duration))
            return false;
        if (exception == null) {
            if (other.exception != null)
                return false;
        } else if (!exception.equals(other.exception))
            return false;
        if (startInstant == null) {
            if (other.startInstant != null)
                return false;
        } else if (!startInstant.equals(other.startInstant))
            return false;
        return true;
    }

    @Override
    public String toString() {
        // Java8 fucked time up again... ChronoUnit.MILLIS throws an exception: http://stackoverflow.com/questions/24491243/why-cant-i-get-a-duration-in-minutes-or-hours-in-java-time
        long ms = duration.get(ChronoUnit.NANOS) / 1000000;
        return "[" + startInstant + ", " + ms + "ms, " + (exception == null ? "success" : exception) + "]";
//        return "TaskExecutionReport [startInstant=" + startInstant
//                + ", duration=" + duration + ", exception=" + exception + "]";
    }
}
