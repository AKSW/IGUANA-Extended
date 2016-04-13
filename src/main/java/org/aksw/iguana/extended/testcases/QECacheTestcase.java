package org.aksw.iguana.extended.testcases;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.extended.testcases.workers.DetailedWorker;
import org.aksw.iguana.testcases.Testcase;
import org.aksw.iguana.testcases.workers.UpdateFileHandler;
import org.aksw.iguana.testcases.workers.Worker.LatencyStrategy;
import org.aksw.iguana.utils.CalendarHandler;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.TimeOutException;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;
import org.w3c.dom.Node;

public class QECacheTestcase implements Testcase {

    private Collection<ResultSet> results = new LinkedList<ResultSet>();
    private Connection con;
    private String conName;
    private Node conNode;
    private String percent;
    private Logger log = Logger.getLogger(QECacheTestcase.class.getSimpleName());
    private String[] prefixes;
    private int workers=1;
    private ExecutorService executor;
    private Map<Integer, DetailedWorker> workerPool = new HashMap<Integer, DetailedWorker>();
    private long timeLimit;
    private Properties prop;

    @Override
    public void start() throws IOException {
        // Init Logger
        initLogger();
        UpdateFileHandler.reset();

        // Init prefixes
        this.prefixes = new String[1];
        this.prefixes[0] = this.workers + "";
        // Init SparqlWorkers
        initWorkers();

        startWorkers();
        // wait time-limit
        waitTimeLimit();
        // getResults
//		makeResults();
//		//
//		saveResults();
//		//
//		cleanMaps();

        //TODO: Save results

        // Stop
        log.info("QECacheTestcase finished");
    }


    private void waitTimeLimit() {
        Calendar start = Calendar.getInstance();
        log.info("Starting QECacheTestcase at: "+CalendarHandler.getFormattedTime(start));
        while((Calendar.getInstance().getTimeInMillis()-start.getTimeInMillis())<timeLimit){
        try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LogHandler.writeStackTrace(log, e, Level.WARNING);
            }
        }
        //Shutdown executor, no other workers can join now
        executor.shutdown();

        for(Integer t : workerPool.keySet()){
            workerPool.get(t).sendEndSignal();
            log.info("Worker: "+t+" will be executed");
        }
        for(Thread t : Thread.getAllStackTraces().keySet()){
            if(t.getName().matches("pool-[0-9]+-thread-[0-9]+")){
                //TODO change Stop Thread with something different
                //Not cool as it's deprecated and in JAVA 8 throws a UnssuportedOPeration Execution
                try{
                    System.out.println(t.getName());
                    t.stop(new TimeOutException());
                }catch(Exception e){
                    log.warning("Thread needed to be stopped");
                }
            }
        }
        while(!executor.isTerminated()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                LogHandler.writeStackTrace(log, e, Level.WARNING);
            }
        }
//		sparqlWorkerPool.clear();
//		updateWorkerPool.clear();
        Calendar end = Calendar.getInstance();
        log.info("QECacheTestcase ended at: "+CalendarHandler.getFormattedTime(end));
        log.info("QECacheTestcase took "+CalendarHandler.getWellFormatDateDiff(start, end));
    }


    protected void startWorkers(){
        log.info("Starting Workers");
        //Starting all workers in new threads
//		executor = Executors.newCachedThreadPool();
        executor = Executors.newFixedThreadPool(workers);
        log.info("Starting now: "+workers+" SPARQL Workers");
        for(Integer i : workerPool.keySet()){
//			log.info("Starting SPARQL Worker "+sparqlWorkerPool.get(i).getWorkerNr());
//			new Thread(sparqlWorkerPool.get(i), "worker-"+i).start();
            executor.submit(workerPool.get(i));

        }
        log.info("All "+workers+" workers have been started");


    }


    protected void initLogger(){
        LogHandler.initLogFileHandler(log , QECacheTestcase.class.getSimpleName());
    }

    protected void initWorkers(){
//		worker.setProps(sparqlProps);
		List<LatencyStrategy> latencyStrategy=new LinkedList<LatencyStrategy>();
		List<Integer[]> latencyAmount = new LinkedList<Integer[]>();
		

		
        for(int i=0;i<workers;i++){
            DetailedWorker worker = new DetailedWorker();
            worker.isPattern(false);
            worker.setLatencyAmount(latencyAmount);
    		worker.setLatencyStrategy(latencyStrategy);
    		worker.setQueriesPath(prop.getProperty("queries-path"));
    		worker.setTimeLimit(timeLimit);
    		worker.setPrefixes(this.prefixes);
    		worker.setConName(conName);
            worker.setConnection(con);
            worker.setWorkerNr(i);
            worker.setProps(prop);
            worker.init(i);
            workerPool.put(i, worker);
        }

    }



    @Override
    public Collection<ResultSet> getResults() {
        return this.results;
    }

    @Override
    public void addCurrentResults(Collection<ResultSet> currentResults) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setProperties(Properties p) {
        //this.props = p;
        //this.timeLimit = p.getProperty("timeLimit");

    }

    @Override
    public void setConnection(Connection con) {
        this.con = con;
    }

    @Override
    public void setConnectionNode(Node con, String id) {
        this.conName = id;
        this.conNode = con;
    }

    @Override
    public void setCurrentDBName(String name) {
        this.conName = name;
    }

    @Override
    public void setCurrentPercent(String percent) {
        this.percent = percent;
    }

    @Override
    public Boolean isOneTest() {
        return false;
    }

}
