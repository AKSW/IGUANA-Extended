# IGUANA-Extended

The [Iguana task runner](src/main/java/org/aksw/iguana/reborn/MainIguanaReborn.java) is now configured with SpringBoot, which allows one to use XML, Java, JSON, YAML and Groovy for configuration purposes. Because of [jena-sparql-api](https://github.com/AKSW/jena-sparql-api)'s fluent API, groovy is probably the most concise choice.

The high level model is pretty straight forward, and comprises these entities:

* [TaskDispatcher](src/main/java/org/aksw/iguana/reborn/TaskDispatcher.java): This class takes an iterator of tasks and passes them sequentially to a task executor. The delay strategy is configurable. Upon completed execution (regardless of sucess or failure) a report is generated and passed on to a consumer. This class only measures the time spent on the task execution and handles execptions.
* The task dispatcher can be simply submitted to an [ExecutorService](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html).
Note, that right now we lack a utility function that sets up approprate task dispatchers and executor services from different task sources. Task sources could be SPARQL queries from a file, from [LSQ](http://lsq.aksw.org/sparql), from a preconfigured array, etc.
* [DefaultTaskReport](src/main/java/org/aksw/iguana/reborn/DefaultTaskReport.java): A report is comprised of the task description, the information gathered from the dispatcher, and the one gathered from the executor.
* [SparqlTaskExecutor](src/main/java/org/aksw/iguana/reborn/SparqlTaskExecutor.java): Instances of this class can be used in conjunction with the TaskDispatcher. Thereby the dispatcher forwards the task objects to the executor's apply(task, reportCallback) method. The reportCallback enables the executor to sent a report back to the dispatcher - even in the event of exceptions. The executor is configured with a QueryExecutionFactory instance that can execute SPARQL queries and a consumption strategy for handling the query responses. The most common consuption strategies are to either retrieve the full result set, or abort execution after retrieval of the first row / triple.



