package org.aksw.iguana.reborn;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.aksw.jena_sparql_api.delay.extra.Delayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskDispatcher<T> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TaskDispatcher.class);

    protected Iterator<T> taskSource;
    protected Consumer<T> taskConsumer;
    protected Delayer delayer;
    protected BiConsumer<T, TaskExecutionReport> reportConsumer;

    public TaskDispatcher(
            Iterator<T> taskSource,
            Consumer<T> taskConsumer,
            Delayer delayer,
            BiConsumer<T, TaskExecutionReport> reportConsumer
            ) {
        super();
        this.taskSource = taskSource;
        this.taskConsumer = taskConsumer;
        this.delayer = delayer;
        this.reportConsumer = reportConsumer;
    }

    @Override
    public void run() {
        //while (
        for(int i = 0; taskSource.hasNext() && !Thread.currentThread().isInterrupted(); ++i) {
            try {
                delayer.doDelay();
                T task = taskSource.next();
                logger.debug("Executing task #" + i + ": " + task);

                Instant startInstant = Instant.now();
                Exception ex = null;
                try {
                    taskConsumer.accept(task);
                } catch(Exception e) {
                    ex = e;
                    logger.warn("Reporting failed task execution", e);
                }
                Instant endInstant = Instant.now();
                Duration duration = Duration.between(startInstant, endInstant);

                TaskExecutionReport report = new TaskExecutionReport(startInstant, duration, ex);

                try {
                    reportConsumer.accept(task, report);
                } catch(Exception e) {
                    logger.error("Failed to send report to consumer", e);
                    throw new RuntimeException(e);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch(Exception e) {
                logger.error("Should never come here", e);
                throw new RuntimeException(e);
            }
        }
    }

}
