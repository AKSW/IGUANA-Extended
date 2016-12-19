package org.aksw.iguana.reborn;

import java.io.IOException;
import java.util.List;

import org.aksw.iguana.reborn.charts.datasets.IguanaDatasetProcessors;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import com.itextpdf.text.DocumentException;

@Controller
public class MainIguanaReborn {

	public static <T> Iterable<T> rotate(List<T> list, int distance) {
		int n = list.size();

		Iterable<T> result;
		if (n == 0) {
			result = list;
		} else {
			distance = distance % n;

			List<T> partA = list.subList(distance, n - distance);
			List<T> partB = list.subList(0, distance);
			result = Iterables.concat(partA, partB);
		}

		return result;
	}

	private static DefaultCategoryDataset createDataset() {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		dataset.addValue(15, "schools", "1970");
		dataset.addValue(30, "schools", "1980");
		dataset.addValue(60, "schools", "1990");
		dataset.addValue(120, "schools", "2000");
		dataset.addValue(240, "schools", "2010");
		dataset.addValue(300, "schools", "2014");
		return dataset;
	}

	/**
	 * Process: - Create a list of queries - Distribute the workload - Measure
	 * the time taken (yield exceptions in case of problems) - Report the
	 * results, deal with exceptions
	 *
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws DocumentException
	 */
	public static void main(String[] args) throws InterruptedException, IOException, DocumentException {
		 ApplicationContext ctx = SpringApplication.run(new
		 Object[]{"file:cache.groovy", ConfigIguanaCore.class}, args);

		 // Query query = QueryFactory.create("SELECT * { { ?s ?p ?o } UNION { ?s
		// ?p ?o } FILTER(?s = <foo>) }", Syntax.syntaxARQ);
		// Op op = Algebra.compile(query);
		// op = Transformer.transform(new TransformFilterPlacement(), op);
		//
		//
		//
		// //Optimize.optimize(op, ARQ.getContext());
		// System.out.println(op);
		//
		// System.exit(0);

		// int workers = 1;
		// ExecutorService executorService = (workers == 1)
		// ? MoreExecutors.newDirectExecutorService()
		// : Executors.newFixedThreadPool(workers)
		// ;
		//
		// executorService.submit(() -> { System.out.println("yay"); });
		//
		//
		// executorService.shutdown();
		// executorService.awaitTermination(1000, TimeUnit.DAYS);
		// System.out.println("Qef: " +
		// ctx.getBean(QueryExecutionFactory.class));

//		JFreeChart lineChart = ChartFactory.createLineChart("Performance", "Years", "Number of Schools",
//				createDataset(), PlotOrientation.VERTICAL, true, true, false);
//
//		// ChartPanel chartPanel = new ChartPanel( lineChart );
//		int width = 500;
//		int height = 400;
//		// chartPanel.setPreferredSize( new java.awt.Dimension(width , height));
//		ChartUtilities2.saveChartAsPDF(new File("/tmp/chart.png"), lineChart, width, height);
//		// setContentPane( chartPanel );

	}
}
