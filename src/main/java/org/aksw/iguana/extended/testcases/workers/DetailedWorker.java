package org.aksw.iguana.extended.testcases.workers;

import java.util.Properties;

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
import org.apache.jena.query.Syntax;

import com.ibm.icu.util.Calendar;

public class DetailedWorker extends SparqlWorker implements Runnable {


    private Properties props;

    @Override
    protected void putResults(Integer time, String queryNr){
        //TODO whatever result metrics are needed.
        //This will put the time needed to request the #queryNr query
    	super.putResults(time, queryNr);
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    @Override
    protected Integer testQuery(String query){
        waitTime();
        int time=-1;

        //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
        //TODO Whatever the executin factory needs
//        QueryExecutionFactory rawQef = FluentQueryExecutionFactory
//                   .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
//                   .config()
//                   .end()
//                   .create();
        QueryExecutionFactory rawQef = FluentQueryExecutionFactory
                //.from(model)
                .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
                //.http("http://localhost:8890/sparql", "http://dbpedia.org")
                .config()
                    .withParser(SparqlQueryParserImpl.create(Syntax.syntaxARQ))
                    .withQueryTransform(F_QueryTransformDatesetDescription.fn)
                    .withPagination(100000)
                .end()
                .create();



//            System.out.println(ResultSetFormatter.asText(rawQef.createQueryExecution(
//              "SELECT * { GRAPH ?g {  ?s <http://ex.org/p1> ?o1 ; <http://ex.org/p2> ?o2 } }").execSelect()));
//            System.out.println("End of test query");

//            QueryExecutionFactory sparqlService = SparqlServiceBuilder
//                    .http("http://akswnc3.informatik.uni-leipzig.de:8860/sparql", "http://dbpedia.org")
//                    .withPagination(100000)
//                    .create();

            //MainSparqlViewCache cache = new MainSparqlViewCache(rawQef);


            QueryExecutionFactory cachedQef = new QueryExecutionFactoryViewCacheMaster(rawQef, OpExecutorFactoryViewCache.get().getServiceMap());

            QueryExecutionFactory mainQef = new QueryExecutionFactoryCompare(rawQef, cachedQef);



        Query q=null;
        try{
            q = QueryFactory.create(query);
        }
        catch(Exception e){
            e.printStackTrace();
            return -1;
        }
        QueryExecution qexec = rawQef.createQueryExecution(q);
        //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
//        Query q = qexec.getQuery();
        log.info("Testing now "+q.toString());
        int qType=q.getQueryType();//q.getQueryType();
        long start = Calendar.getInstance().getTimeInMillis();
        //TODO if their is something to do with the results
        switch(qType){
        case Query.QueryTypeAsk:
            qexec.execAsk();
            break;
        case Query.QueryTypeConstruct:
            qexec.execConstruct();
            break;
        case Query.QueryTypeDescribe:
            qexec.execDescribe();
            break;
        case Query.QueryTypeSelect:
            qexec.execSelect();
            break;
        }

        long end = Calendar.getInstance().getTimeInMillis();
        time=Long.valueOf(end-start).intValue();
        return time;
    }

}
