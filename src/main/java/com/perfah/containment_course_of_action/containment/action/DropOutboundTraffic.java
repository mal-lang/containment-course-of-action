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
import org.apache.tinkerpop.gremlin.structure.T;

public class DropOutboundTraffic extends ContainmentAction {
    @Role(description = "The application that should not be able to send network packets.", assetType = "Application")
    public long sender; 

    public DropOutboundTraffic(){}

    public DropOutboundTraffic(long sender){
        this.sender = sender;
    }

    @Override
    public String getName(){
        return "DropOutboundTraffic";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Drop outbound network packets (by port, IP / MAC-address, egress filtering, etc..) from sender %s (a firewall / application / network).",
            GraphUtil.getAssetRefStr(g, sender));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(sender)
                .has("metaConcept", "Application")
                .union(
                    __.outE("appConnections", "outgoingAppConnections")
                      .property(CSAF.ASSOC_SWAP, deployed ? "ingoingAppConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("applications", "outApplications")
                      .filter(__.inV().hasId(sender))
                      .property(CSAF.ASSOC_SWAP, deployed ? "inApplications" : ""),

                    __.outE("netConnections", "outgoingNetConnections")
                      .property(CSAF.ASSOC_SWAP, deployed ? "ingoingNetConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("networks", "outNetworks", "diodeOutNetworks")
                      .filter(__.inV().hasId(sender))
                      .property(CSAF.ASSOC_SWAP, deployed ? "inNetworks" : "")
                )
                .hasNext();           
    }
}
