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

public class SuspendHost extends ContainmentAction {
    @Role(description = "A ConnectionRule", assetType = "System")
    public long host; 

    public SuspendHost(){}

    public SuspendHost(long connection){
        this.host = connection;
    }

    @Override
    public String getName(){
        return "SuspendHost";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Suspend (shutdown, hibernate, put to sleep, etc...) host %s", 
            GraphUtil.getAssetRefStr(g, host));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(host)
                .has("metaConcept", "System")
                .property(CSAF.ASSET_DETACH, deployed)
                .union(
                    __.out("sysData")
                      .property(CSAF.ASSET_DETACH, deployed),

                    __.repeat(__.out("sysExecutedApps")).until(__.out("sysExecutedApps").count().is(0))
                      .union(
                        __.identity(),
                        __.repeat(__.out("appExecutedApps")).until(__.out("appExecutedApps").count().is(0))
                      )
                      .has("metaConcept", "Application")
                      .property(CSAF.ASSET_DETACH, deployed)
                      .union(
                        __.identity(),
    
                        __.out("containedData", "transitData")
                          .where(__.out("containingApp", "transitApp").count().is(1))
                          .property(CSAF.ASSET_DETACH, deployed)
                      )
                )
                .hasNext();
    }
}
