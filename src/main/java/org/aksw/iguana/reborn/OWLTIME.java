package org.aksw.iguana.reborn;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class OWLTIME {
    public static final String ns = "http://www.w3.org/2006/time#";

    public static Resource resource(String local) { return ResourceFactory.createResource(ns + local); }
    public static Property property(String local) { return ResourceFactory.createProperty(ns + local); }

    // Used internally for the hypergraph representation - not part of the public vocab
    public static final Property hasDuration = property(ns + "hasDuration");
    public static final Property numericDuration = property(ns + "numericDuration");
}