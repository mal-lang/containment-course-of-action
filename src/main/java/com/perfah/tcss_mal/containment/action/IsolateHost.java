package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.containment.CSAF;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;

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