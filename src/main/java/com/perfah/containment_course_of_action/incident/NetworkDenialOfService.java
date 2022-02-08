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
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.perfah.containment_course_of_action.util.GraphUtil;
import com.perfah.containment_course_of_action.util.Role;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import com.perfah.containment_course_of_action.containment.action.*;
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
