package org.aksw.iguana.reborn;

import java.io.StringWriter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.aksw.jena_sparql_api.delay.extra.Delayer;
import org.aksw.simba.lsq.vocab.LSQ;
import org.aksw.simba.lsq.vocab.PROV;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDFS;
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
 * @param <T> The type of the task object
 * @param <R> The type of the task result
 */
public class TaskDispatcher<T>
    implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(TaskDispatcher.class);

    //protected Iterator<T> taskSource;
    protected Iterator<Resource> taskSource;
    protected Function<Resource, T> taskToEntity;
    //protected Function<? super ExecutionContext<T>, ? extends String> taskToExecutionIriStr;

    //protected BiConsumer<T, Consumer<? super E>> taskConsumer;
    protected BiConsumer<T, Resource> taskConsumer;
    //protected BiFunction<T, Resource, R> taskConsumer;
    //protected TriConsumer<T, Resource, R> postProcessor;
    protected TriConsumer<T, Resource, Exception> exceptionHandler;
    //protected Consumer<DefaultTaskReport<T, TaskTimeReport, E>> reportConsumer;
    protected Consumer<Resource> reportConsumer;

    protected Delayer delayer;

    //protected ExecutorService executorService;

    public TaskDispatcher(
            //Iterator<T> taskSource,
            Iterator<Resource> taskSource,
            Function<Resource, T> taskToEntity,
            //Function<? super ExecutionContext<T>, ? extends String> taskToExecutionIriStr,
            //BiConsumer<T, Consumer<? super E>> taskConsumer,
            BiConsumer<T, Resource> taskConsumer,
            //BiFunction<T, Resource, R> taskConsumer,
            //TriConsumer<T, Resource, R> postProcessor,
            TriConsumer<T, Resource, Exception> exceptionHandler,
            Consumer<Resource> reportConsumer,
            Delayer delayer
            //Consumer<DefaultTaskReport<T, TaskTimeReport, E>> reportConsumer
            ) {
        super();
        this.taskSource = taskSource;
        this.taskToEntity = taskToEntity;
        //this.taskToExecutionIriStr = taskToExecutionIriStr;
        this.taskConsumer = taskConsumer;
        //this.postProcessor = postProcessor;
        this.exceptionHandler = exceptionHandler;
        this.reportConsumer = reportConsumer;
        this.delayer = delayer;
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        try {
            //while (
            for(int i = 0; taskSource.hasNext() && !Thread.currentThread().isInterrupted(); ++i) {
                Resource task = taskSource.next();
                try {
                    //Model m = ModelFactory.createDefaultModel();
                    //String prefix = "http://ex.org/";
                    //Resource r = m.createResource(prefix + "task-execution-" + i);

                    delayer.doDelay();
                    //T task = taskSource.next();
                    logger.debug("Executing task #" + i + ": " + task);

                    T t = taskToEntity.apply(task);

                    int mode = 2;
                    if(mode == 0) {
                        Thread thread = new Thread(() -> execute(task, t));
                        thread.start();

                        Thread.sleep(2000);

                        if(thread.isAlive()) {
                            //System.out.println("TIMEOUT - Forcefully killing thread");
                            thread.stop();
                            throw new TimeoutException();
                        }
                    } else if(mode == 1) {
                        Future<?> future = executorService.submit(() -> execute(task, t));
                        future.get(20, TimeUnit.SECONDS);
                        //executorService.submit(() -> System.out.println("i got called"));
                    } else {
                        execute(task, t);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (TimeoutException e) {
                    task.addLiteral(RDFS.comment, "TIMEOUT");
                } catch(Exception e) {
                    logger.error("Should never come here", e);
                    throw new RuntimeException(e);
                }

                //task.getModel().write(System.out, "TURTLE");
            }
        } finally {
            executorService.shutdown();
        }
    }

    public void execute(Resource task, T t) {

        if(task.getProperty(OWLTIME.numericDuration) != null) {
            StringWriter tmp = new StringWriter();
            ResourceUtils.reachableClosure(task).write(tmp, "TTL");
            throw new RuntimeException("Task " + task + " already has a numeric duration assigned: " + tmp);
        }

        //Instant startInstant = Instant.now();
        Calendar startInstant = new GregorianCalendar();
        //ExecutionContext<T> ec = new ExecutionContext<>(task, t, startInstant);

//    	String executionIriStr = taskToExecutionIriStr.apply(ec); //task, startInstant);
//    	Resource r = m.createResource(executionIriStr);
        //task.addProperty(PROV.wasAssociatedWith, task);

        task.addLiteral(PROV.startedAtTime, startInstant);
        // Use guava as it uses System.nanoTime()
        Stopwatch sw = Stopwatch.createStarted();
        //Exception ex = null;

        //Holder<E> executorReportHolder = new Holder<>();
        try {
            taskConsumer.accept(t, task); //(e) -> { executorReportHolder.setValue(e); });
//            R result = taskConsumer.apply(t, task); //(e) -> { executorReportHolder.setValue(e); });

            //postProcessor.accept(t, task, result);

        } catch(Exception e) {
            //ex = e;
            logger.warn("Reporting failed task execution", e);
            task.addLiteral(LSQ.executionError, "" + e);

            exceptionHandler.accept(t,  task, e);
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
    }

}
