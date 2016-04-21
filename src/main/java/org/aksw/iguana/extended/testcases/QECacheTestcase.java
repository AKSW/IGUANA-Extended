package org.aksw.iguana.extended.testcases;

import java.io.File;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.benchmark.processor.ResultProcessor;
import org.aksw.iguana.extended.testcases.workers.DetailedWorker;
import org.aksw.iguana.testcases.StressTestcase;
import org.aksw.iguana.testcases.Testcase;
import org.aksw.iguana.testcases.workers.UpdateFileHandler;
import org.aksw.iguana.testcases.workers.Worker.LatencyStrategy;
import org.aksw.iguana.utils.CalendarHandler;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.TimeOutException;
import org.aksw.jena_sparql_api.compare.QueryExecutionFactoryCompare;
import org.aksw.jena_sparql_api.concept_cache.core.OpExecutorFactoryViewCache;
import org.aksw.jena_sparql_api.concept_cache.core.QueryExecutionFactoryViewCacheMaster;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.utils.transform.F_QueryTransformDatesetDescription;
import org.apache.jena.query.Syntax;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;
import org.w3c.dom.Node;

public class QECacheTestcase implements Testcase {

    static {
        OpExecutorFactoryViewCache.registerGlobally();
    }
	
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
	private int pagination=100000;
	

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
		makeResults();
//		//
		saveResults();
//		//
		cleanMaps();

        //TODO: Save results

        // Stop
        log.info("QECacheTestcase finished");
    }

    
    protected void saveResults() {
		for(ResultSet res : results){
			String fileName = res.getFileName();
			String[] prefixes = res.getPrefixes();
			String suffix="";
			for(String prefix : prefixes){
				suffix+=prefix+File.separator;
			}
			String path = "."+File.separator+
					ResultProcessor.getTempResultFolder()+
					File.separator+QECacheTestcase.class.getName().replace(".", "-")+
					File.separator+suffix;
			new File(path).mkdirs();
			res.setFileName(path+fileName);
			try {
				res.save();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
			res.setFileName(fileName);
		}
	}
    

	private void cleanMaps() {
		workerPool.clear();
//		updateWorkerPool.clear();
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


    private void makeResults(){
		List<List<ResultSet>> sparqlResults = new LinkedList<List<ResultSet>>();
		for(Integer key : workerPool.keySet()){
			List<ResultSet> res = (List<ResultSet>) workerPool.get(key).makeResults();
			sparqlResults.add(res);
			results.addAll(res);
		}
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
		
		QueryExecutionFactory rawQef = FluentQueryExecutionFactory
                .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
                .config()
                    .withParser(SparqlQueryParserImpl.create(Syntax.syntaxARQ))
                    .withQueryTransform(F_QueryTransformDatesetDescription.fn)
                    .withPagination(pagination)
                    .withPostProcessor(qe -> qe.setTimeout(3, TimeUnit.SECONDS))
                .end()
                .create();


        QueryExecutionFactory cachedQef = new QueryExecutionFactoryViewCacheMaster(
                rawQef, OpExecutorFactoryViewCache.get().getServiceMap());

        QueryExecutionFactory mainQef = new QueryExecutionFactoryCompare(
                rawQef, cachedQef);
		
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
            worker.setQef(mainQef);
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
        this.prop = p;
        this.timeLimit = Long.valueOf(p.getProperty("timeLimit"));
        if(p.contains("pagination")){
        	pagination = Integer.valueOf(p.getProperty("pagination"));
        }
        
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
