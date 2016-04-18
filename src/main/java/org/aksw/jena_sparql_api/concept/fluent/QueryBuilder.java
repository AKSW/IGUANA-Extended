package org.aksw.jena_sparql_api.concept.fluent;

/**
 * This query model comprises
 * - conceptBuilder for selecting a set of resources
 * - projectionBuilder for selecting what to yield for the resources
 * - orderBuilder for ordering the result 
 * 
 * @author raven
 *
 */
public class QueryBuilder {
    protected ConceptBuilder conceptBuilder;
    protected ProjectionBuilder projectionBuilder;
    protected OrderBuilder orderBuilder;
}
