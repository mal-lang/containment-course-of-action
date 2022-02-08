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

public class BlockSpecificConnection extends ContainmentAction {
    @Role(description = "The connection to block.", assetType = "ConnectionRule")
    public long connection;

    public BlockSpecificConnection(){}

    public BlockSpecificConnection(long connection){
        super();
        this.connection = connection;
    }

    @Override
    public String getName(){
        return "BlockSpecificConnection";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Block a specific connection %s (by physical unplug, port, IP / MAC-address, ingress / egress filtering, etc..)",
            GraphUtil.getAssetRefStr(g, connection));
    }
    @Override
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(connection)
                .has("metaConcept", "ConnectionRule")
                .property(CSAF.ASSET_DETACH, deployed)
                .hasNext();
    }
}
