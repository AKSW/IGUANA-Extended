package org.aksw.iguana.reborn;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;

public class SparqlTaskConsumer
    implements Consumer<String>
{
    protected QueryExecutionFactory qef;

    /**
     * The consumption strategy
     *
     * Predefined ones:
     * QueryExecutionUtils::consume
     * QueryExecutionUtils::abortAfterFirstRow
     *
     */
    protected Consumer<QueryExecution> queryExecutionConsumer;

    public SparqlTaskConsumer(
            QueryExecutionFactory qef,
            Consumer<QueryExecution> queryExecutionConsumer) {
        super();
        this.qef = qef;
        this.queryExecutionConsumer = queryExecutionConsumer;
    }

    @Override
    public void accept(String queryStr) {
        QueryExecution qe = qef.createQueryExecution(queryStr);
        queryExecutionConsumer.accept(qe);
    }

}
