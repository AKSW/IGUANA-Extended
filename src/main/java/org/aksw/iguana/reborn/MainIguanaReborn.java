package org.aksw.iguana.reborn;

import java.util.List;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
public class MainIguanaReborn {
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
