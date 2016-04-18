package org.aksw.iguana.reborn;

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
     */
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(new Object[]{ConfigIguanaCore.class, "file:beans.groovy"}, args);
        System.out.println("Qef: " + ctx.getBean(QueryExecutionFactory.class));
    }
}
