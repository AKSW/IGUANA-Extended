package org.aksw.iguana

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import org.aksw.iguana.reborn.SparqlTaskConsumer
import org.aksw.iguana.reborn.TaskDispatcher
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory
import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDecorator
import org.aksw.jena_sparql_api.core.utils.QueryExecutionUtils
import org.aksw.jena_sparql_api.delay.extra.DelayerDefault
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl
import org.aksw.jena_sparql_api.utils.transform.F_QueryTransformDatesetDescription
import org.apache.jena.ext.com.google.common.util.concurrent.MoreExecutors
import org.apache.jena.query.Query
import org.apache.jena.query.Syntax
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.sparql.core.Var

import com.google.common.collect.Lists


String queryQueryStr = """
    PREFIX lsqv: <http://lsq.aksw.org/vocab#>
    PREFIX sp: <http://spinrdf.org/sp#>
    PREFIX dct:<http://purl.org/dc/terms/>
    SELECT DISTINCT ?queryStr ?id {
      ?id
        a sp:Select ; # Can also use other forms
        sp:text ?queryStr ;
        lsqv:resultSize ?rs ;
        lsqv:triplePatterns ?tp;
        lsqv:bgps ?bgps ;
        lsqv:execution ?executions .

      ?executions
        dct:issued  ?time

        FILTER(NOT EXISTS {
          ?id lsqv:usesFeature ?o
          FILTER(?o IN (lsqv:Filter, lsqv:Distinct, lsqv:Optional, lsqv:Union))
        })
        FILTER (?rs > 0 && ?bgps = 1)
    }
""".stripIndent()


SparqlQueryParser queryParser = SparqlQueryParserImpl.create(Syntax.syntaxARQ)

Query queryQuery = queryParser.apply(queryQueryStr)
queryQuery.setLimit(100)


QueryExecutionFactory lsqQef = FluentQueryExecutionFactory
    .http("http://lsq.aksw.org/sparql", "http://dbpedia.org")
    .config()
        .withParser(queryParser)
        .withPagination(1000)
    .end()
    .create()


// TODO Wrap this ugly part up in some util function
List<String> queries = new ArrayList<String>()
List<Var> projectVars = queryQuery.getProjectVars()
Var queryVar = projectVars.get(0)
if(queryVar == null) {
    throw new RuntimeException("At least on query variable expected")
}
String queryVarName = "" + queryVar
lsqQef.createQueryExecution(queryQuery).execSelect().forEachRemaining {
    RDFNode queryNode = it.get(queryVarName)
    String queryStr = "" + queryNode;
    queries.add(queryStr.replace("\\n", " "))
}


QueryExecutionFactory dataQef = FluentQueryExecutionFactory
    //.from(model)
    .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
    //.http("http://localhost:8890/sparql", "http://dbpedia.org")
    .config()
        .withParser(queryParser)
        .withQueryTransform(F_QueryTransformDatesetDescription.fn)
        .withPagination(100000)
        .withPostProcessor({ it.setTimeout(10, TimeUnit.SECONDS) }) // Max 10 seconds execution time per query
    .end()
    .create()

// TODO Assure value range validity of workers
int workers = 1

List<List<String>> partitions = Lists.partition(queries, workers);

ExecutorService executorService = workers == 1
    ? MoreExecutors.newDirectExecutorService()
    : Executors.newFixedThreadPool(workers)
    ;


// Alternative consumption strategy: QueryExecutionUtils.&abortAfterFirstRow
// Note: .& is groovy's equivalent to java8's :: - it creates a closure from a method
SparqlTaskExecutor sparqlTaskExecutor = new SparqlTaskExecutor(dataQef, QueryExecutionUtils.&consume);

// Note: delay in ms, should upgrade this class to Java8
TaskDispatcher taskDispatcher =
    new TaskDispatcher(
        queries.iterator(),
        sparqlTaskExecutor,
        new DelayerDefault(1000), // ms
        { println("" + it) }); // report callback

executorService.submit(taskDispatcher)

println("Shutting down executor service")
executorService.shutdown()
executorService.awaitTermination(1, TimeUnit.DAYS);


// Actually not needed so far, but here we can pass beans back to the application
beans {
    qef QueryExecutionFactoryDecorator, dataQef
}

