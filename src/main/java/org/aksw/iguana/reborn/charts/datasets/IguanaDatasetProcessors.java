package org.aksw.iguana.reborn.charts.datasets;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.util.function.Function;

import org.aksw.iguana.reborn.OWLTIME;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.prefix.core.QueryTransformPrefix;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.simba.lsq.vocab.LSQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.engine.binding.Binding;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.SubCategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.StandardGradientPaintTransformer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class IguanaDatasetProcessors {

	public static void main(String[] args) {

		Model m = RDFDataMgr.loadModel("/home/raven/tmp/iguana-test-data.ttl");




		PrefixMapping pm = new PrefixMappingImpl();
		pm.setNsPrefixes(PrefixMappingImpl.Extended);
		pm.setNsPrefix("ex", "http://example.org/ontology#");
		pm.setNsPrefix("ig", IguanaVocab.ns);
		pm.setNsPrefix("lsq", LSQ.ns);
		pm.setNsPrefix("time", OWLTIME.ns);
		pm.setNsPrefix("qb", "http://datacube.org/ontology#");


		Function<Query, Query> queryTransform = new QueryTransformPrefix(pm);

		QueryExecutionFactory qef = FluentQueryExecutionFactory.from(m)
				.config()
					.withQueryTransform(queryTransform)
					.withParser(SparqlQueryParserImpl.create(Syntax.syntaxARQ, new Prologue(pm)))
				.end()
				.create();



		createDataset(m, qef);

	}


	/**
	 * The problemis, that we would have to generate URIs for (workload, series) combinations...
	 * But actually, we could for each run just attach a series attribute.
	 *
	 *
	 * Compute a set of mean values for
	 * - a given set of observations
	 * - projected onto a related set of resourecs
	 * - with each grouping corresponding to a data series
	 * - having measure property 'measure'
	 *
	 * Select ?s (Count(DISTINCT ?run) / SUM(?runTime) As ?avg) {
	 *   observations(?o)
	 *   projections(?o -> ?s)
	 *   dataseries(?o -> ?d)
	 *
	 *
     * } Group by ?d
     *
	 *
	 *
	 * @param model
	 * @param observations
	 * @param subjects
	 * @param groupings
	 * @param measure
	 */
//	public static void enrichMean(Model model, Concept observations, Relation projection, Relation measure, Relation series) {
//		ConceptUtils.createRelatedConcept(obs, projection);
//	}

	/**
	 * Allocate the URIs for the data series and attach the id atributes
	 * x A Series ; x workloadId ?w ; x seriesId
	 */
	public static final String computeAvg = String.join("\n",
			"CONSTRUCT {",
			"  ?s",
			"    qb:dataset ?d ;",
			"    ig:workload ?w ;",
			"    ig:avg ?avg ;",
			"    .",
			"",
			"}",
			"{",
			"  { SELECT (IRI(CONCAT('http://avg-', STR(?did), '-', STR(?wid))) AS ?s) ?d ?w (AVG(?m) AS ?avg) {",
			"    ?x",
			"      ig:workload ?w ;",
			"      qb:dataset ?d ;",
			"      time:numericDuration ?m ;",
			"    .",
			"    ?d rdfs:label ?did .",
			"    ?w ig:queryId ?wid .",
			"  } GROUP BY ?d ?w ?did ?wid}",
			"}");

//	/**
//	 * Computes the average for each workload over all series (in all experiments)
//	 *
//	 *
//	 */
//	public static final String computeAvg = String.join("\n",
//		"CONSTRUCT {",
//		"    ?i ig:avg ?avg",
//		"}",
//		"{",
//		"  { SELECT (IRI(CONCAT('http://foo-', STR(?i), '-', STR(?r))) AS ?s) ?i ?r (AVG(?m) AS ?avg) {",
//		"    ?x",
//		"      ig:workload ?i ;",
//		"      time:numericDuration ?m ;",
//		"      .",
//		"  } GROUP BY ?i ?r }",
//		"}");


	public static final String computeStdDev = String.join("\n",
		"CONSTRUCT {",
		"    ?i ig:stdDeviation ?stdDev",
		"}",
		"{",
		"  { SELECT ?i (SUM(?sqErr) AS ?stdDev) {",
		"    ?i",
		"      ig:avg ?a ;",
		"      qb:dataset ?d ;",
		"      ig:workload ?w ;",
		"    .",
		"    ?x",
		"      qb:dataset ?d ;",
		"      ig:workload ?w ;",
		"      time:numericDuration ?m ;",
		"      .",
		"    BIND(?m - ?a AS ?err)",
		"    BIND(?err * ?err AS ?sqErr)",
		"  } GROUP BY ?i }",
		"}");

	/**
	 * Prepare data for a stacked bar-chart
	 * for each query id, with different execution strategy, the taken time
	 *
	 *
	 * @param model
	 * @return
	 */
	public static CategoryDataset createDataset(Model model, QueryExecutionFactory qef) {//Model model) {
		// (avgExecutionTime, executorLabel, queryId)
		// Create the avg execution time for each executor

		qef.createQueryExecution("CONSTRUCT { ex:DefaultDataset rdfs:label \"Cached\" } { }").execConstruct(model);
		qef.createQueryExecution("CONSTRUCT { ?x qb:dataset ex:DefaultDataset } { ?x ig:run ?r }").execConstruct(model);

		qef.createQueryExecution(computeAvg).execConstruct(model);
		qef.createQueryExecution(computeStdDev).execConstruct(model);

		model.write(System.out, "TURTLE");

				if(true) { return null; }

		String queryStr = String.join("\n",
			"SELECT ?r ?i ?m ?c {",
			"  ?s",
			"    ig:run ?r ;",
			"    ig:workload/ig:queryId ?i ;", // ; lsq:text ?w]",
			"    time:numericDuration ?m ;",
			//"    ex:executor/rdfs:label ?c ;",
			"    .",
			"}");

		// standardabweichung := summe der quadratischen abweichungen vom mittelwert

		//QueryExecutionFactoryQueryTransform

		// get result vars and interpret them according to the order
		QueryExecution qe = qef.createQueryExecution(queryStr);
		ResultSet rs = qe.execSelect();

//
		System.out.println("results:");
		while(rs.hasNext()) {
			Binding b = rs.nextBinding();
			System.out.println(b);
			//b.get(var);
		}
		System.out.println("end of results.");

		// Given: [runId] [queryId] [executionId] [executorId]

		// Goal: [avg] [stdev] [queryId] [executorId]

		// For each query id, count the number of runs and the sum of the execution times, then divide

		/*
		 * Bind (?runTime - ?avg As ?x)
		 * Bind(?x * ?x As ?dev)
		 */

		Multimap<RDFNode, Resource> catToResources = HashMultimap.create();



		DefaultStatisticalCategoryDataset result = new DefaultStatisticalCategoryDataset();
		//result.

		// 1. For each query, create
		return null;
	}

	//http://images.google.de/imgres?imgurl=http://www.java2s.com/Code/JavaImages/JFreeChartStatisticalBarChartDemo.PNG&imgrefurl=http://www.java2s.com/Code/Java/Chart/JFreeChartStatisticalBarChartDemo.htm&h=297&w=508&tbnid=9JNURsr-PnNVWM:&tbnh=90&tbnw=154&docid=jCsU5GotaaiaSM&client=ubuntu&usg=__QaSkWXuVLpOoM-mE0Jb2Ivj3p8U=&sa=X&ved=0ahUKEwjVoP772dPPAhVIlCwKHXaBBd0Q9QEINjAE
//	final DefaultStatisticalCategoryDataset result = new DefaultStatisticalCategoryDataset();
//	result.add(32.5, 17.9, "Series 1", "Type 1");


    /**
     * Creates a sample dataset.
     *
     * @return A sample dataset.
     */
    private CategoryDataset createDataset() {
        DefaultCategoryDataset result = new DefaultCategoryDataset();

        result.addValue(20.3, "Product 1 (US)", "Jan 04");
        result.addValue(27.2, "Product 1 (US)", "Feb 04");
        result.addValue(19.7, "Product 1 (US)", "Mar 04");
        result.addValue(19.4, "Product 1 (Europe)", "Jan 04");
        result.addValue(10.9, "Product 1 (Europe)", "Feb 04");
        result.addValue(18.4, "Product 1 (Europe)", "Mar 04");
        result.addValue(16.5, "Product 1 (Asia)", "Jan 04");
        result.addValue(15.9, "Product 1 (Asia)", "Feb 04");
        result.addValue(16.1, "Product 1 (Asia)", "Mar 04");
        result.addValue(13.2, "Product 1 (Middle East)", "Jan 04");
        result.addValue(14.4, "Product 1 (Middle East)", "Feb 04");
        result.addValue(13.7, "Product 1 (Middle East)", "Mar 04");

        result.addValue(23.3, "Product 2 (US)", "Jan 04");
        result.addValue(16.2, "Product 2 (US)", "Feb 04");
        result.addValue(28.7, "Product 2 (US)", "Mar 04");
        result.addValue(12.7, "Product 2 (Europe)", "Jan 04");
        result.addValue(17.9, "Product 2 (Europe)", "Feb 04");
        result.addValue(12.6, "Product 2 (Europe)", "Mar 04");
        result.addValue(15.4, "Product 2 (Asia)", "Jan 04");
        result.addValue(21.0, "Product 2 (Asia)", "Feb 04");
        result.addValue(11.1, "Product 2 (Asia)", "Mar 04");
        result.addValue(23.8, "Product 2 (Middle East)", "Jan 04");
        result.addValue(23.4, "Product 2 (Middle East)", "Feb 04");
        result.addValue(19.3, "Product 2 (Middle East)", "Mar 04");

        result.addValue(11.9, "Product 3 (US)", "Jan 04");
        result.addValue(31.0, "Product 3 (US)", "Feb 04");
        result.addValue(22.7, "Product 3 (US)", "Mar 04");
        result.addValue(15.3, "Product 3 (Europe)", "Jan 04");
        result.addValue(14.4, "Product 3 (Europe)", "Feb 04");
        result.addValue(25.3, "Product 3 (Europe)", "Mar 04");
        result.addValue(23.9, "Product 3 (Asia)", "Jan 04");
        result.addValue(19.0, "Product 3 (Asia)", "Feb 04");
        result.addValue(10.1, "Product 3 (Asia)", "Mar 04");
        result.addValue(13.2, "Product 3 (Middle East)", "Jan 04");
        result.addValue(15.5, "Product 3 (Middle East)", "Feb 04");
        result.addValue(10.1, "Product 3 (Middle East)", "Mar 04");

        return result;
    }

    /**
     * Creates a sample chart.
     *
     * @param dataset  the dataset for the chart.
     *
     * @return A sample chart.
     */
    private JFreeChart createChart(final CategoryDataset dataset) {

        final JFreeChart chart = ChartFactory.createStackedBarChart(
            "Stacked Bar Chart Demo 4",  // chart title
            "Category",                  // domain axis label
            "Value",                     // range axis label
            dataset,                     // data
            PlotOrientation.VERTICAL,    // the plot orientation
            true,                        // legend
            true,                        // tooltips
            false                        // urls
        );

        GroupedStackedBarRenderer renderer = new GroupedStackedBarRenderer();
        KeyToGroupMap map = new KeyToGroupMap("G1");
        map.mapKeyToGroup("Product 1 (US)", "G1");
        map.mapKeyToGroup("Product 1 (Europe)", "G1");
        map.mapKeyToGroup("Product 1 (Asia)", "G1");
        map.mapKeyToGroup("Product 1 (Middle East)", "G1");
        map.mapKeyToGroup("Product 2 (US)", "G2");
        map.mapKeyToGroup("Product 2 (Europe)", "G2");
        map.mapKeyToGroup("Product 2 (Asia)", "G2");
        map.mapKeyToGroup("Product 2 (Middle East)", "G2");
        map.mapKeyToGroup("Product 3 (US)", "G3");
        map.mapKeyToGroup("Product 3 (Europe)", "G3");
        map.mapKeyToGroup("Product 3 (Asia)", "G3");
        map.mapKeyToGroup("Product 3 (Middle East)", "G3");
        renderer.setSeriesToGroupMap(map);

        renderer.setItemMargin(0.0);
        Paint p1 = new GradientPaint(
            0.0f, 0.0f, new Color(0x22, 0x22, 0xFF), 0.0f, 0.0f, new Color(0x88, 0x88, 0xFF)
        );
        renderer.setSeriesPaint(0, p1);
        renderer.setSeriesPaint(4, p1);
        renderer.setSeriesPaint(8, p1);

        Paint p2 = new GradientPaint(
            0.0f, 0.0f, new Color(0x22, 0xFF, 0x22), 0.0f, 0.0f, new Color(0x88, 0xFF, 0x88)
        );
        renderer.setSeriesPaint(1, p2);
        renderer.setSeriesPaint(5, p2);
        renderer.setSeriesPaint(9, p2);

        Paint p3 = new GradientPaint(
            0.0f, 0.0f, new Color(0xFF, 0x22, 0x22), 0.0f, 0.0f, new Color(0xFF, 0x88, 0x88)
        );
        renderer.setSeriesPaint(2, p3);
        renderer.setSeriesPaint(6, p3);
        renderer.setSeriesPaint(10, p3);

        Paint p4 = new GradientPaint(
            0.0f, 0.0f, new Color(0xFF, 0xFF, 0x22), 0.0f, 0.0f, new Color(0xFF, 0xFF, 0x88)
        );
        renderer.setSeriesPaint(3, p4);
        renderer.setSeriesPaint(7, p4);
        renderer.setSeriesPaint(11, p4);
        renderer.setGradientPaintTransformer(
            new StandardGradientPaintTransformer(GradientPaintTransformType.HORIZONTAL)
        );

        SubCategoryAxis domainAxis = new SubCategoryAxis("Product / Month");
        domainAxis.setCategoryMargin(0.05);
        domainAxis.addSubCategory("Product 1");
        domainAxis.addSubCategory("Product 2");
        domainAxis.addSubCategory("Product 3");

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setDomainAxis(domainAxis);
        //plot.setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
        plot.setRenderer(renderer);
        plot.setFixedLegendItems(createLegendItems());
        return chart;

    }

    /**
     * Creates the legend items for the chart.  In this case, we set them manually because we
     * only want legend items for a subset of the data series.
     *
     * @return The legend items.
     */
    private LegendItemCollection createLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
//        LegendItem item1 = new LegendItem("US", new Color(0x22, 0x22, 0xFF));
  //      LegendItem item2 = new LegendItem("Europe", new Color(0x22, 0xFF, 0x22));
    //    LegendItem item3 = new LegendItem("Asia", new Color(0xFF, 0x22, 0x22));
      //  LegendItem item4 = new LegendItem("Middle East", new Color(0xFF, 0xFF, 0x22));
//        result.add(item1);
  //      result.add(item2);
    //    result.add(item3);
      //  result.add(item4);
        return result;
    }

    // ****************************************************************************
    // * JFREECHART DEVELOPER GUIDE                                               *
    // * The JFreeChart Developer Guide, written by David Gilbert, is available   *
    // * to purchase from Object Refinery Limited:                                *
    // *                                                                          *
    // * http://www.object-refinery.com/jfreechart/guide.html                     *
    // *                                                                          *
    // * Sales are used to provide funding for the JFreeChart project - please    *
    // * support us so that we can continue developing free software.             *
    // ****************************************************************************

//    /**
//     * Starting point for the demonstration application.
//     *
//     * @param args  ignored.
//     */
//    public static void main(final String[] args) {
////        final StackedBarChartDemo4 demo = new StackedBarChartDemo4("Stacked Bar Chart Demo 4");
////        demo.pack();
////        RefineryUtilities.centerFrameOnScreen(demo);
////        demo.setVisible(true);
//    }

}
