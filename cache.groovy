package org.aksw.iguana

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import org.aksw.iguana.reborn.SparqlTaskExecutor
import org.aksw.iguana.reborn.TaskDispatcher
import org.aksw.jena_sparql_api.compare.QueryExecutionFactoryCompare
import org.aksw.jena_sparql_api.concept_cache.core.JenaExtensionViewCache
import org.aksw.jena_sparql_api.concept_cache.core.OpExecutorFactoryViewCache
import org.aksw.jena_sparql_api.concept_cache.core.QueryExecutionFactoryViewCacheMaster
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

import com.google.common.collect.Iterables
import com.google.gson.Gson
import com.google.gson.GsonBuilder


JenaExtensionViewCache.register()


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
          FILTER(?o IN (lsqv:Filter, lsqv:Distinct, lsqv:Optional, lsqv:Union, lsqv:Minus))
        })
        FILTER (?rs > 0 && ?bgps = 1)
    }
""".stripIndent()


//queryQueryStr = """
//    PREFIX lsqv: <http://lsq.aksw.org/vocab#>
//    PREFIX sp: <http://spinrdf.org/sp#>
//    PREFIX dct:<http://purl.org/dc/terms/>
//    SELECT DISTINCT ?queryStr ?id {
//      ?id
//        a sp:Select ; # Can also use other forms
//        sp:text ?queryStr ;
//        lsqv:bgps ?bgps
//
//        FILTER(NOT EXISTS {
//          ?id lsqv:usesFeature ?o
//          FILTER(?o IN (lsqv:Optional, lsqv:Union, lsqv:Minus))
//        })
//
//        FILTER (?bgps = 1)
//    }
//""".stripIndent()

SparqlQueryParser queryParser = SparqlQueryParserImpl.create(Syntax.syntaxARQ)

Query queryQuery = queryParser.apply(queryQueryStr)
queryQuery.setOffset(0)
queryQuery.setLimit(250)

println "Here"
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
    String queryStr = "" + queryNode
    queryStr = queryStr.replace("\\n", " ")
    queries.add(queryStr)
}

println "Writing file..."
Files.write(Paths.get("queries.out"), queries);



// Write the queries to a file
Gson gson = new GsonBuilder().setPrettyPrinting().create()
String content = gson.toJson(queries)
Files.write(Paths.get("./bgp-queries.json"), content.getBytes())



Iterable<Query> tasks = Iterables.concat(queries, queries, queries)

//QueryExecutionFactory dataQef = FluentQueryExecutionFactory
//    //.from(model)
//    .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
//    //.http("http://localhost:8890/sparql", "http://dbpedia.org")
//    .config()
//        .withParser(queryParser)
//        .withQueryTransform(F_QueryTransformDatesetDescription.fn)
//        .withPagination(100000)
//        .withPostProcessor({ it.setTimeout(10, TimeUnit.SECONDS) }) // Max 10 seconds execution time per query
//    .end()
//    .create()


QueryExecutionFactory rawQef = FluentQueryExecutionFactory
//.from(model)
.http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
//.http("http://localhost:8890/sparql", "http://dbpedia.org")
.config()
    .withParser(SparqlQueryParserImpl.create(Syntax.syntaxARQ))
    .withQueryTransform(F_QueryTransformDatesetDescription.fn)
    .withPagination(100000)
.end()
.create()


QueryExecutionFactory cachedQef = new QueryExecutionFactoryViewCacheMaster(rawQef, OpExecutorFactoryViewCache.get().getServiceMap())
QueryExecutionFactory dataQef = new QueryExecutionFactoryCompare(rawQef, cachedQef)





// TODO Assure value range validity of workers
int workers = 1

//List<List<String>> partitions = Lists.partition(queries, workers)

//Random rand = new Random();
//List<String> subList = queries.subList(rand.nextInt());
//partitions.add(subList);

ExecutorService executorService = (workers == 1
    ? MoreExecutors.newDirectExecutorService()
    : Executors.newFixedThreadPool(workers))



// Alternative consumption strategy: QueryExecutionUtils.&abortAfterFirstRow
// Note: .& is groovy's equivalent to java8's :: - it creates a closure from a method
SparqlTaskExecutor sparqlTaskExecutor = new SparqlTaskExecutor(dataQef, QueryExecutionUtils.&consume)

// Note: delay in ms, should upgrade this class to Java8
TaskDispatcher taskDispatcher =
    new TaskDispatcher(
        tasks.iterator(),
        sparqlTaskExecutor,
        new DelayerDefault(0), // ms
        { println("" + it) }) // report callback


List<Runnable<?>> runnables = Collections.singletonList(taskDispatcher);

List<Callable<?>> callables = runnables.stream().map({r -> Executors.callable(r)}).collect(Collectors.toList())

List<Future<?>> futures = executorService.invokeAll(callables)

println("Shutting down executor service")
executorService.shutdown()
executorService.awaitTermination(1, TimeUnit.DAYS)

for(Future<?> future : futures) {
    try {
        future.get()
    } catch(Exception ex) {
        ex.printStackTrace()
    }
}


// Actually not needed so far, but here we can pass beans back to the application
beans {
    qef QueryExecutionFactoryDecorator, dataQef
    myString String, "hello world"
}

