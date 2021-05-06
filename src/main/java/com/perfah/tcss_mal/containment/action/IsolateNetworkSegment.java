package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.util.DefenseFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;

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
