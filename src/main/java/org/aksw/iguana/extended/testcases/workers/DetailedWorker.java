package org.aksw.iguana.extended.testcases.workers;

import java.util.Properties;

import org.aksw.iguana.testcases.workers.SparqlWorker;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.ibm.icu.util.Calendar;

public class DetailedWorker extends SparqlWorker implements Runnable {


    private Properties props;

    @Override
    protected void putResults(Integer time, String queryNr){
        //TODO whatever result metrics are needed.
        //This will put the time needed to request the #queryNr query
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
        QueryExecutionFactory rawQef = FluentQueryExecutionFactory
                   .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
                   .config()
                   .end()
                   .create();
        QueryExecution qexec = rawQef.createQueryExecution(query);
        //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        int qType=qexec.getQuery().getQueryType();
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
