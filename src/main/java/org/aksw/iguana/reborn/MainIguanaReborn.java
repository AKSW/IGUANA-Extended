package org.aksw.iguana.reborn;

import java.util.List;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.optimize.TransformFilterPlacement;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
public class MainIguanaReborn {

    public static <T> Iterable<T> rotate(List<T> list, int distance) {
        int n = list.size();

        Iterable<T> result;
        if(n == 0)  {
            result = list;
        } else {
            distance = distance % n;

            List<T> partA = list.subList(distance, n - distance);
            List<T> partB = list.subList(0, distance);
            result = Iterables.concat(partA, partB);
        }

        return result;
    }

    /**
     * Process:
     *  - Create a list of queries
     *  - Distribute the workload
     *  - Measure the time taken (yield exceptions in case of problems)
     *  - Report the results, deal with exceptions
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {

//        Query query = QueryFactory.create("SELECT * { { ?s ?p ?o } UNION { ?s ?p ?o } FILTER(?s = <foo>) }", Syntax.syntaxARQ);
//        Op op = Algebra.compile(query);
//        op = Transformer.transform(new TransformFilterPlacement(), op);
//
//
//
//        //Optimize.optimize(op, ARQ.getContext());
//        System.out.println(op);
//
//        System.exit(0);

//        int workers = 1;
//        ExecutorService executorService = (workers == 1)
//                ? MoreExecutors.newDirectExecutorService()
//                : Executors.newFixedThreadPool(workers)
//                ;
//
//        executorService.submit(() -> { System.out.println("yay"); });
//
//
//        executorService.shutdown();
//        executorService.awaitTermination(1000, TimeUnit.DAYS);
        ApplicationContext ctx = SpringApplication.run(new Object[]{"file:cache.groovy", ConfigIguanaCore.class}, args);
        System.out.println("Qef: " + ctx.getBean(QueryExecutionFactory.class));
    }
}
