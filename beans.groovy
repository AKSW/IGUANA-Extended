package org.aksw.iguana

import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory
import org.aksw.jena_sparql_api.core.QueryExecutionFactory
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDecorator
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl
import org.aksw.jena_sparql_api.utils.transform.F_QueryTransformDatesetDescription
import org.apache.jena.query.Query
import org.apache.jena.query.Syntax


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

Query lsqQuery = queryParser.apply(queryQueryStr)
QueryExecutionFactory lsqQef = FluentQueryExecutionFactory
    .http("http://lsq.aksw.org/sparql", "http://dbpedia.org")
    .config()
        .withParser(queryParser)
        .withPagination(1000)
    .end()
    .create()

//lsqQef.createQueryExecution(queryQueryStr).execSelect().forEachRemaining(
//
//)



QueryExecutionFactory dataQef = FluentQueryExecutionFactory
    //.from(model)
    .http("http://akswnc3.informatik.uni-leipzig.de/data/dbpedia/sparql", "http://dbpedia.org")
    //.http("http://localhost:8890/sparql", "http://dbpedia.org")
    .config()
        .withParser(queryParser)
        .withQueryTransform(F_QueryTransformDatesetDescription.fn)
        .withPagination(100000)
    .end()
    .create()


beans {
    qef QueryExecutionFactoryDecorator, dataQef
}

