package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.util.DefenseFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class RedirectTraffic extends ContainmentAction {
    @Role(description = "A ", assetType = "Application")
    private long regularApp;

    @Role(description = "The app ", assetType = "Application")
    private long substituteApp;

    public RedirectTraffic(long connection){
        this.regularApp = connection;
    }

    @Override
    public String getName(){
        return "RedirectTraffic";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Redirect traffic targetting %s to %s", 
            GraphUtil.getAssetRefStr(g, regularApp));
    }

    @Override
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(regularApp)
                .has("metaConcept", "Application")
                .property(DefenseFlag.DISABLE, !deployed)
                .in("outgoingAppConnections", "outgoingNetConnections",
                    "ingoingAppConnections", "ingoingNetConnections",
                    "appConnections", "netConnections")
                .hasNext();
    }
}
