package org.aksw.iguana.reborn;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.aksw.jena_sparql_api.delay.extra.Delayer;
import org.aksw.simba.lsq.vocab.LSQ;
import org.aksw.simba.lsq.vocab.PROV;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;


/**
 * A dispatcher reads tasks from a source and passes them on to an executor.
 * Thereby, the time of the task execution is measured.
 * Implements Runnable so it can be easily run in a thread.
 *
 * A lambda is used to construct an IRI string for the task execution from the task description.
 * Furthermore, a function can be provided, that turns a task description into a Java entity
 * that gets passed to the executor. This conversion does not count towards the execution.
 *
 * @author raven
 *
 * @param <T>
 * @param <E>
 */
public class TaskDispatcher<T, E>
    implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(TaskDispatcher.class);

    //protected Iterator<T> taskSource;
    protected Iterator<Resource> taskSource;
    protected Function<Resource, T> taskToEntity;
    //protected Function<? super ExecutionContext<T>, ? extends String> taskToExecutionIriStr;

    //protected BiConsumer<T, Consumer<? super E>> taskConsumer;
    protected BiConsumer<T, Resource> taskConsumer;
    protected Delayer delayer;
    //protected Consumer<DefaultTaskReport<T, TaskTimeReport, E>> reportConsumer;
    protected Consumer<Resource> reportConsumer;

    public TaskDispatcher(
            //Iterator<T> taskSource,
    		Iterator<Resource> taskSource,
    		Function<Resource, T> taskToEntity,
    		//Function<? super ExecutionContext<T>, ? extends String> taskToExecutionIriStr,
            //BiConsumer<T, Consumer<? super E>> taskConsumer,
            BiConsumer<T, Resource> taskConsumer,
            Delayer delayer,
            //Consumer<DefaultTaskReport<T, TaskTimeReport, E>> reportConsumer
            Consumer<Resource> reportConsumer
            ) {
        super();
        this.taskSource = taskSource;
        this.taskToEntity = taskToEntity;
        //this.taskToExecutionIriStr = taskToExecutionIriStr;
        this.taskConsumer = taskConsumer;
        this.delayer = delayer;
        this.reportConsumer = reportConsumer;
    }

    @Override
    public void run() {
        //while (
        for(int i = 0; taskSource.hasNext() && !Thread.currentThread().isInterrupted(); ++i) {
            try {
                //Model m = ModelFactory.createDefaultModel();
            	//String prefix = "http://ex.org/";
            	//Resource r = m.createResource(prefix + "task-execution-" + i);

                delayer.doDelay();
                //T task = taskSource.next();
                Resource task = taskSource.next();
                logger.debug("Executing task #" + i + ": " + task);

                T t = taskToEntity.apply(task);

                //Instant startInstant = Instant.now();
                Calendar startInstant = new GregorianCalendar();
                ExecutionContext<T> ec = new ExecutionContext<>(task, t, startInstant);

//            	String executionIriStr = taskToExecutionIriStr.apply(ec); //task, startInstant);
//            	Resource r = m.createResource(executionIriStr);
            	//task.addProperty(PROV.wasAssociatedWith, task);

                task.addLiteral(PROV.startedAtTime, startInstant);
                // Use guava as it uses System.nanoTime()
                Stopwatch sw = Stopwatch.createStarted();
                //Exception ex = null;

                //Holder<E> executorReportHolder = new Holder<>();
                try {
                    taskConsumer.accept(t, task); //(e) -> { executorReportHolder.setValue(e); });
                } catch(Exception e) {
                    //ex = e;
                    logger.warn("Reporting failed task execution", e);
                    task.addLiteral(LSQ.executionError, "" + e);
                }

                //Instant endInstant = Instant.now();
                //Duration duration = Duration.between(startInstant, endInstant);
                sw.stop();
                Calendar stopInstant = new GregorianCalendar();
                Duration duration = Duration.ofNanos(sw.elapsed(TimeUnit.NANOSECONDS));
                //duration.get(ChronoUnit.MILLIS);


                //TaskTimeReport dispatcherReport = new TaskTimeReport(startInstant, duration, ex);
                //Resource d = r.getModel().createResource(prefix + "task-execution-duration" + i);
                task.addLiteral(PROV.endAtTime, stopInstant);
                task.addLiteral(OWLTIME.numericDuration, duration.get(ChronoUnit.NANOS) / 1000000000.0);
                //r.addProperty(OWLTIME.hasDuration, )

                try {
                    //reportConsumer.accept(fullReport);
                	reportConsumer.accept(task);
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
