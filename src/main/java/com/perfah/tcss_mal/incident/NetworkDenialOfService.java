package com.perfah.tcss_mal.incident;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import com.perfah.tcss_mal.containment.action.*;
import org.apache.tinkerpop.gremlin.structure.T;

public class NetworkDenialOfService extends Incident {
    @Role private long attacker, target;

    public NetworkDenialOfService(){}

    public NetworkDenialOfService(long attacker, long target) {
        this.attacker = attacker;
        this.target = target;
    }

    @Override
    public String getName() {
        return "NetworkDenialOfService";
    }

    public String getDescription(GraphTraversalSource g){
        return String.format("A network denial of service attack by %s targeting network/connection %s.",
            GraphUtil.getAssetRefStr(g, attacker),
            GraphUtil.getAssetRefStr(g, target));
    }

    @Override
    public String getAttackStepCorrelate(){
        return target + "." + "denialOfService";
    }


    @Override
    public List<Incident> instantiate(GraphTraversalSource g) {
        return g.V()
            .or(__.has("metaConcept", "ConnectionRule"), __.has("metaConcept", "Network"))
            .as("T")
            .out("denialOfService.attacker")
            .has("metaConcept", "Attacker")
            .as("A")
            .select("T", "A")
            .by(T.id)
            .toStream()
            .map(x -> (Incident) new NetworkDenialOfService(
                (long)x.get("A"),
                (long)x.get("T")
            ))
            .collect(Collectors.toList());
    }
}
