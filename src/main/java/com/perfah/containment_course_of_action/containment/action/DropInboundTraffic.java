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

import com.perfah.containment_course_of_action.containment.CSAF;
import com.perfah.containment_course_of_action.util.DefenseFlag;
import com.perfah.containment_course_of_action.util.GraphUtil;
import com.perfah.containment_course_of_action.util.Role;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

public class DropInboundTraffic extends ContainmentAction {
    @Role(description = "The application that should not be able to receive network packets.", assetType = "Application")
    public long receiver; 

    public DropInboundTraffic(long receiver){
        this.receiver = receiver;
    }

    public DropInboundTraffic(){}

    @Override
    public String getName(){
        return "DropInboundTraffic";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Drop inbound network packets (by port, IP / MAC-address, ingress filtering, etc..) sent to receiver %s (a firewall / application / network).",
            GraphUtil.getAssetRefStr(g, receiver));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(receiver)
                .has("metaConcept", "Application")
                .union(
                    __.outE("appConnections", "ingoingAppConnections")
                      .property(CSAF.ASSOC_SWAP, deployed ? "outgoingAppConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("applications", "inApplications")
                      .where(__.inV().hasId(receiver))
                      .property(CSAF.ASSOC_SWAP, deployed ? "outApplications" : ""),

                    __.outE("netConnections", "ingoingNetConnections")
                      .property(CSAF.ASSOC_SWAP, deployed ? "outgoingNetConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("networks", "inNetworks")
                      .where(__.inV().hasId(receiver))
                      .property(CSAF.ASSOC_SWAP, deployed ? "outNetworks" : "")
                )
                .hasNext(); 
    }
}

// "diodeIngoingNetConnections"