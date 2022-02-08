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

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;

/*

PhysicalZone     [physicalZone]      0..1 <-- ZoneInclusion         --> *   [systems]                System
Identity         [highPrivSysIds]       * <-- HighPrivilegeAccess   --> *   [highPrivManagedSystems] System
Identity         [lowPrivSysIds]        * <-- LowPrivilegeAccess    --> *   [lowPrivManagedSystems]  System
Group            [highPrivSysGroups]    * <-- HighPrivilegeAccess   --> *   [highPrivManagedSystems] System
Group            [lowPrivSysGroups]     * <-- LowPrivilegeAccess    --> *   [lowPrivManagedSystems]  System
*/

public class IsolateHost extends ContainmentAction {
    @Role(description = "The host to isolate", assetType = "System")
    public long host;

    public IsolateHost(long connection){
        this.host = connection;
    }

    public IsolateHost(){}

    @Override
    public String getName(){
        return "IsolateHost";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Isolate host %s from network (by unplugging physical cables, blocking all connections in the firewall or router, etc.. )", 
            GraphUtil.getAssetRefStr(g, host));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(host)
                .has("metaConcept", "System")
                .repeat(__.out("sysExecutedApps")).until(__.out("sysExecutedApps").count().is(0))
                .union(
                    __.identity(),
                    __.repeat(__.out("appExecutedApps")).until(__.out("appExecutedApps").count().is(0))
                )
                .has("metaConcept", "Application")
                .as("apps")
                .outE("appConnections", 
                     "ingoingAppConnections",
                     "outgoingAppConnections",
                     "networks",
                     "clientAccessNetworks")
                .property(CSAF.ASSOC_DETACH, deployed)
                .inV()
                .outE("applications", 
                    "inApplications",
                    "outApplications",
                    "clientApplications")
                .filter(__.inV().id().as("returnApp").select("apps").by(T.id).where(P.eq("returnApp")))
                .property(CSAF.ASSOC_DETACH, deployed)
                .hasNext();
    }
}

// !Should transitApp / transitData be included?