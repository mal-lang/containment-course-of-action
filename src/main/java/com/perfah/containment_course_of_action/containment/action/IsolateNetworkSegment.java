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

package com.perfah.containment_course_of_action.containment.action;

import com.perfah.containment_course_of_action.util.DefenseFlag;
import com.perfah.containment_course_of_action.util.GraphUtil;
import com.perfah.containment_course_of_action.util.Role;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class IsolateNetworkSegment extends ContainmentAction {
    @Role(description = "The network that should be disconnect from other networks.", assetType = "Network")
    private long networkSegment;

    public IsolateNetworkSegment(long networkSegment){
        this.networkSegment = networkSegment;
    }

    @Override
    public String getName(){
        return "IsolateNetworkSegment";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Isolate the network segment %s from all other networks (by unplugging physical cables, blocking all connections in the firewall or router, etc.. )", 
            GraphUtil.getAssetRefStr(g, networkSegment));
    }

    @Override
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(networkSegment)
                .out("netConnections", "ingoingNetConnections", "outgoingNetConnections")
                .has("metaConcept", "ConnectionRule")
                .out("networks", "outNetworks", "inNetworks", "diodeInNetworks", "routingFirewalls")
                .has(T.id, P.neq(networkSegment))
                .property(DefenseFlag.DISABLE, deployed)
                .hasNext();
    }
}
