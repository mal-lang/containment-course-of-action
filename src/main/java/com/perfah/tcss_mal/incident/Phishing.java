package com.perfah.tcss_mal.incident;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.*;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
import com.perfah.tcss_mal.util.SupportedIntervention;

import org.apache.tinkerpop.gremlin.structure.T;
import com.perfah.tcss_mal.incident.*;
import com.perfah.tcss_mal.containment.action.*;
import com.perfah.tcss_mal.containment.evaluation.*;

public class Phishing extends Incident {
    @Role private long attacker, user, identity, credentials;

    public Phishing() {}

    private Phishing(long attacker, long user, long identity, long credentials){
        this.attacker = attacker;
        this.user = user;
        this.identity = identity;
        this.credentials = credentials;
    }

    @Override
    public String getName() {
        return "Phishing";
    }

    @Override
    public String getDescription(GraphTraversalSource g){
        //return "User " + user + " may be phished for credentials " + credentials;
        return "User " + GraphUtil.getAssetRefStr(g, user) + " reports they’ve fallen for a phishing email";
    }

    @Override
    public String getAttackStepCorrelate(){
        return user + "." + "phishUser";
    }

    @Override
    public List<Incident> instantiate(GraphTraversalSource g) {
        return g.V()
            .has("metaConcept", "Attacker")
            .as("A")
            .in("attemptSocialEngineering.attacker", "phishUser.attacker")
            .has("metaConcept", "User")
            .as("U")
            .in("users")
            .has("metaConcept","Identity")
            .as("I")
            .out("credentials")
            .has("metaConcept", "Credentials")
            .as("C")
            .select("A", "U", "I", "C")
            .by(T.id)
            .toStream()
            .map((x) -> (Incident) new Phishing(
                (long)x.get("A"),
                (long)x.get("U"), 
                (long)x.get("I"),
                (long)x.get("C")
            ))
            .collect(Collectors.toList());
    }
}
