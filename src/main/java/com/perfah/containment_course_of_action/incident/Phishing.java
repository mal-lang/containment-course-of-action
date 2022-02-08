// Copyright 2022 Per Fahlander
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.perfah.containment_course_of_action.incident;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.*;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.perfah.containment_course_of_action.util.GraphUtil;
import com.perfah.containment_course_of_action.util.Role;
import com.perfah.containment_course_of_action.util.SupportedIntervention;

import org.apache.tinkerpop.gremlin.structure.T;
import com.perfah.containment_course_of_action.incident.*;
import com.perfah.containment_course_of_action.containment.action.*;
import com.perfah.containment_course_of_action.containment.strategy.*;

public class Phishing extends Incident {
    @Role private long attacker, user, identity;

    public Phishing() {}

    private Phishing(long attacker, long user, long identity){
        this.attacker = attacker;
        this.user = user;
        this.identity = identity;
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
            .in("phishUser.attacker", "attemptSocialEngineering.attacker")
            .has("metaConcept", "User")
            .as("U")
            .in("users")
            .has("metaConcept","Identity")
            .as("I")
            .select("A", "U", "I")
            .by(T.id)
            .toStream()
            .map((x) -> (Incident) new Phishing(
                (long)x.get("A"),
                (long)x.get("U"), 
                (long)x.get("I")
            ))
            .collect(Collectors.toList());
    }
}
