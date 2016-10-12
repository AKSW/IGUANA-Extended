package org.aksw.iguana.reborn.charts.datasets;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class IguanaVocab {
    public static final String ns = "http://www.w3.org/2006/time#";

    public static Resource resource(String local) { return ResourceFactory.createResource(ns + local); }
    public static Property property(String local) { return ResourceFactory.createProperty(ns + local); }

    // Used internally for the hypergraph representation - not part of the public vocab
    public static final Property queryId = property(ns + "queryId");
    public static final Property workload = property(ns + "workload");
    //public static final Property numericDuration = property(ns + "numericDuration");


}