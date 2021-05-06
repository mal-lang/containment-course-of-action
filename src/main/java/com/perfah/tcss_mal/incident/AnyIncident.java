package com.perfah.tcss_mal.incident;

import java.util.List;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class AnyIncident extends Incident {
    
    @Override
    public String getName() {
        return "Any";
    }


    public String getDescription(GraphTraversalSource g){
        return "A wildcard that represents all types of attack points";
    }

    @Override
    public String getAttackStepCorrelate(){
        return "*.*";
    }

    @Override
    public List<Incident> instantiate(GraphTraversalSource g) {
        return List.of(new AnyIncident());
    }
}
