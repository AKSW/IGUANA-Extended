package org.aksw.iguana.reborn;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.simba.lsq.vocab.LSQ;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Resource;

public class SparqlTaskExecutor
    implements BiConsumer<String, Resource>
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
    protected Function<? super QueryExecution, ? extends Number> queryExecutionConsumer;

    public SparqlTaskExecutor(
            QueryExecutionFactory qef,
            Function<? super QueryExecution, ? extends Number> queryExecutionConsumer) {
        super();
        this.qef = qef;
        this.queryExecutionConsumer = queryExecutionConsumer;
    }

    @Override
    public void accept(String queryStr, Resource out) {
        QueryExecution qe = qef.createQueryExecution(queryStr);
        Number processedItems = queryExecutionConsumer.apply(qe);
        out.addLiteral(LSQ.resultSize, processedItems);
    }

}
