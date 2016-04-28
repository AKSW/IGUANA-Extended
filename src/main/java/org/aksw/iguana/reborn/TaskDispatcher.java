package org.aksw.iguana.reborn;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.aksw.jena_sparql_api.delay.extra.Delayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskDispatcher<T, E>
    implements Runnable
{

    private static final Logger logger = LoggerFactory.getLogger(TaskDispatcher.class);

    protected Iterator<T> taskSource;
    protected BiConsumer<T, Consumer<? super E>> taskConsumer;
    protected Delayer delayer;
    protected Consumer<DefaultTaskReport<T, TaskTimeReport, E>> reportConsumer;

    public TaskDispatcher(
            Iterator<T> taskSource,
            BiConsumer<T, Consumer<? super E>> taskConsumer,
            Delayer delayer,
            Consumer<DefaultTaskReport<T, TaskTimeReport, E>> reportConsumer
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

                Holder<E> executorReportHolder = new Holder<>();
                try {
                    taskConsumer.accept(task, (e) -> { executorReportHolder.setValue(e); });
                } catch(Exception e) {
                    ex = e;
                    logger.warn("Reporting failed task execution", e);
                }

                Instant endInstant = Instant.now();
                Duration duration = Duration.between(startInstant, endInstant);
                //duration.get(ChronoUnit.MILLIS);

                TaskTimeReport dispatcherReport = new TaskTimeReport(startInstant, duration, ex);
                E executorReport = executorReportHolder.getValue();
                DefaultTaskReport<T, TaskTimeReport, E> fullReport = new DefaultTaskReport<>(task, dispatcherReport, executorReport);

                try {
                    reportConsumer.accept(fullReport);
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
