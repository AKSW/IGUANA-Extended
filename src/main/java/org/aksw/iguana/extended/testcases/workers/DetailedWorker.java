package org.aksw.iguana.extended.testcases.workers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.aksw.iguana.testcases.workers.SparqlWorker;
import org.aksw.jena_sparql_api.compare.QueryExecutionFactoryCompare;
import org.aksw.jena_sparql_api.concept_cache.core.OpExecutorFactoryViewCache;
import org.aksw.jena_sparql_api.concept_cache.core.QueryExecutionFactoryViewCacheMaster;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.utils.transform.F_QueryTransformDatesetDescription;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.bio_gene.wookie.utils.LogHandler;


public class DetailedWorker extends SparqlWorker implements Runnable {

    static {
        OpExecutorFactoryViewCache.registerGlobally();
    }

    private static final String DIR_STRING = "DetailedWorker"
            + UUID.randomUUID().toString();
    private Properties props;
    private Boolean cache = true;

    @Override
    protected void putResults(Integer time, String queryNr) {
        // TODO whatever result metrics are needed.
        // This will put the time needed to request the #queryNr query
        // super.putResults(time, queryNr);
        int oldTime = 0;
        if (resultMap.containsKey(queryNr)) {
            oldTime = resultMap.get(queryNr);
        }
        File dir = new File(DIR_STRING);
        dir.mkdir();
        if (time < 0) {
            log.warning("Query " + queryNr
                    + " wasn't successfull for connection " + getConName()
                    + ". See logs for more inforamtion");
            log.warning("This will be saved as failed query");
            time = 0;
            inccMap(queryNr, failMap);
        } else {
            inccMap(queryNr, succMap);
        }
        File f = new File(DIR_STRING + File.separator + queryNr + ".det");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                log.severe("Couldn't create file " + f.getAbsolutePath());
                LogHandler.writeStackTrace(log, e, Level.SEVERE);
            }
        }
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(f, true))) {
            pw.println(time + "");
        } catch (FileNotFoundException e) {
            log.severe("Couldn't find file " + f.getAbsolutePath());
        }

        resultMap.put(queryNr, oldTime + time);
    }

    private void inccMap(String queryNr, Map<String, Integer> map) {
        int incc = 0;
        if (map.containsKey(queryNr)) {
            incc = map.get(queryNr);
        }
        map.put(queryNr, incc + 1);
    }

    public void setProps(Properties props) {
        this.props = props;
        if (props.getProperty("cache") != null)
            this.cache = Boolean.valueOf(props.getProperty("cache"));
    }

    @Override
    protected Integer testQuery(String query) {
        if (cache) {
            return testQueryCached(query);
        }
        return testQueryStd(query);
    }

    private Integer testQueryStd(String query) {
        waitTime();
        int time = -1;

        QueryExecutionFactory rawQef = FluentQueryExecutionFactory
                .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql",
                        "http://dbpedia.org")
                .config()
                    .withParser(SparqlQueryParserImpl.create(Syntax.syntaxARQ))
                    .withQueryTransform(F_QueryTransformDatesetDescription.fn)
                    .withPagination(100000)
                    .withPostProcessor(qe -> qe.setTimeout(3, TimeUnit.SECONDS))
                .end()
                .create();

        Query q = null;
        q = QueryFactory.create(query);
        QueryExecution qexec = rawQef.createQueryExecution(q);
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        // Query q = qexec.getQuery();
        log.info("Testing now " + query);
        return testQueryQE(q, qexec);
    }

    private Integer testQueryCached(String query) {
        waitTime();
        int time = -1;

        QueryExecutionFactory rawQef = FluentQueryExecutionFactory
                .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
                .config()
                    .withParser(SparqlQueryParserImpl.create(Syntax.syntaxARQ))
                    .withQueryTransform(F_QueryTransformDatesetDescription.fn)
                    .withPagination(100000)
                    .withPostProcessor(qe -> qe.setTimeout(3, TimeUnit.SECONDS))
                .end()
                .create();


        QueryExecutionFactory cachedQef = new QueryExecutionFactoryViewCacheMaster(
                rawQef, OpExecutorFactoryViewCache.get().getServiceMap());

        QueryExecutionFactory mainQef = new QueryExecutionFactoryCompare(
                rawQef, cachedQef);

        //Query q = null;
        //q = QueryFactory.create(query);
        QueryExecution qexec = mainQef.createQueryExecution(query);

        log.info("Testing now " + query);
        return testQueryQE(qexec.getQuery(), qexec);
    }

    private Integer testQueryQE(Query q, QueryExecution qexec){
        int qType = q.getQueryType();// q.getQueryType();
        long start = Calendar.getInstance().getTimeInMillis();
        // TODO if their is something to do with the results
        Integer time=-1;
        switch (qType) {
        case Query.QueryTypeAsk:
            qexec.execAsk();
            break;
        case Query.QueryTypeConstruct:
            Model m = qexec.execConstruct();
            break;
        case Query.QueryTypeDescribe:
            m = qexec.execDescribe();
            break;
        case Query.QueryTypeSelect:
            ResultSet res = qexec.execSelect();
            if(res==null)
                return -1;
            try{
                ResultSetFormatter.consume(res);
            }catch(Exception e){
                log.warning("");
                LogHandler.writeStackTrace(log, e, Level.WARNING);
                return -1;
            }
//			res.hasNext();
            break;
        }
        long end = Calendar.getInstance().getTimeInMillis();
        time = Long.valueOf(end - start).intValue();

        return time;
    }

}
