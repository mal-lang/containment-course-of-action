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
import com.perfah.containment_course_of_action.util.GraphUtil;
import com.perfah.containment_course_of_action.util.Role;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

public class StopService extends ContainmentAction {
    @Role(description = "", assetType = "Application")
    public long service; 

    public StopService(long service){
        this.service = service;
    }

    public StopService(){}

    
    @Override
    public String getName(){
        return "StopService";
    }
    
    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Stop service %s (by killing the application, etc...)", 
            GraphUtil.getAssetRefStr(g, service));
        }    

    @Override
    @SuppressWarnings("unchecked")
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(service)
                .has("metaConcept", "Application")    
                .union(
                    __.identity(),
                    __.repeat(__.out("appExecutedApps")).until(__.out("appExecutedApps").count().is(0))
                )
                .property(CSAF.ASSET_DETACH, deployed)
                .union(
                    __.identity(),

                    __.out("containedData", "transitData")
                      .where(__.out("containingApp", "transitApp").count().is(1))
                      .property(CSAF.ASSET_DETACH, deployed)
                )
                .hasNext();
    }
}