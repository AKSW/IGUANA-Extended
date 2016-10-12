package org.aksw.iguana

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.IntStream

import org.aksw.iguana.reborn.SparqlTaskExecutor
import org.aksw.iguana.reborn.TaskDispatcher
import org.aksw.iguana.reborn.charts.datasets.IguanaVocab
import org.aksw.jena_sparql_api.compare.QueryExecutionFactoryCompare
import org.aksw.jena_sparql_api.concept_cache.core.JenaExtensionViewMatcher
import org.aksw.jena_sparql_api.concept_cache.core.QueryExecutionFactoryViewMatcherMaster
import org.aksw.jena_sparql_api.concept_cache.core.StorageEntry
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory
import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDecorator
import org.aksw.jena_sparql_api.core.utils.QueryExecutionUtils
import org.aksw.jena_sparql_api.delay.extra.DelayerDefault
import org.aksw.jena_sparql_api.parse.QueryExecutionFactoryParse
import org.aksw.jena_sparql_api.remap.QueryExecutionFactoryRemap
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl
import org.aksw.jena_sparql_api.utils.transform.ElementTransformDatasetDescription
import org.aksw.simba.lsq.vocab.LSQ
import org.apache.jena.query.Query
import org.apache.jena.query.Syntax
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.core.Var
import org.apache.jena.util.ResourceUtils

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.GsonBuilder


JenaExtensionViewMatcher.register()


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

Model tasksModel = ModelFactory.createDefaultModel();
List<Resource> tasks = new ArrayList<>()
for(int i = 0; i < queries.size; ++i) {
	String query = queries.get(i)
	Resource t = tasksModel.createResource("http://example.org/query-" + i)
	t.addLiteral(LSQ.text, tasksModel.createLiteral(query))
	t.addLiteral(IguanaVocab.queryId, i)

	tasks.add(t)
}

tasksModel.write(System.out, "TURTLE")

// Create tasks from the queries
Iterator<Resource> taskExecs = IntStream.range(0, 5).boxed().flatMap({ runId ->
	tasks.stream().map({it -> [runId, it]})
}).map({it ->
  Model m = ModelFactory.createDefaultModel()
  Resource x = it[1]
  Model n = ResourceUtils.reachableClosure(x)
  m.add(n)
  x = x.inModel(m)
  long queryId = x.getRequiredProperty(IguanaVocab.queryId).getObject().asLiteral().getLong()
  def r = m.createResource("http://example.org/query-" + queryId + "-run-" + it[0])
  r.addProperty(IguanaVocab.workload, x)
  r
  //m.write(System.out, "TURTLE")
}).iterator()

//Function<ExecutionContext, String> createExecutionIriStr = { ec ->
//	long taskId = ec.getTaskResource().getRequiredProperty(IguanaVocab.queryId).getObject().asLiteral().getLong()
//   "http://example.org/task-execution-" + run + "-" + taskId
//};

Function<Resource, Query> taskToEntity = { r ->
	r.getRequiredProperty(IguanaVocab.workload).getObject().asResource().getRequiredProperty(LSQ.text).getObject().asLiteral().getString()
//	queryParser.apply(queryStr)
}
//Iterable<Query> tasks = Iterables.concat(queries, queries, queries)

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
    .withQueryTransform(ElementTransformDatasetDescription.&rewrite)
    .withPagination(50000)
	.withDefaultLimit(50000, true)
.end()
.create()



CacheBuilder<Object, Object> queryCacheBuilder = CacheBuilder.newBuilder()
	.maximumSize(10000);

ExecutorService cacheExecutorService = Executors.newCachedThreadPool();

QueryExecutionFactoryViewMatcherMaster tmp = QueryExecutionFactoryViewMatcherMaster.create(rawQef, queryCacheBuilder, cacheExecutorService);
Cache<Node, StorageEntry> queryCache = tmp.getCache();
rawQef = tmp
rawQef = new QueryExecutionFactoryRemap(rawQef);

QueryExecutionFactory cachedQef = new QueryExecutionFactoryParse(rawQef, SparqlQueryParserImpl.create());


QueryExecutionFactory dataQef = new QueryExecutionFactoryCompare(cachedQef, rawQef)










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
        taskExecs,//tasks.iterator(),
		taskToEntity,
        sparqlTaskExecutor,
        new DelayerDefault(0), // ms
        { it.getModel().write(System.out, "TURTLE") }) // report callback


List<Runnable<?>> runnables = Collections.singletonList(taskDispatcher);

List<Callable<?>> callables = runnables.stream()
	.map({r -> Executors.callable(r)})
	.collect(Collectors.toList())

List<Future<?>> futures = executorService.invokeAll(callables)

println("Shutting down executor service")
executorService.shutdown()
executorService.awaitTermination(5, TimeUnit.SECONDS)

cacheExecutorService.shutdown()
cacheExecutorService.awaitTermination(5, TimeUnit.SECONDS)

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

