package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.containment.StructuralModifierAttribute;
import com.perfah.tcss_mal.util.DefenseFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
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
                      .property(StructuralModifierAttribute.ASSOC_REPLACEMENT, deployed ? "outgoingAppConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("applications", "inApplications")
                      .where(__.inV().hasId(receiver))
                      .property(StructuralModifierAttribute.ASSOC_REPLACEMENT, deployed ? "outApplications" : ""),

                    __.outE("netConnections", "ingoingNetConnections")
                      .property(StructuralModifierAttribute.ASSOC_REPLACEMENT, deployed ? "outgoingNetConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("networks", "inNetworks")
                      .where(__.inV().hasId(receiver))
                      .property(StructuralModifierAttribute.ASSOC_REPLACEMENT, deployed ? "outNetworks" : "")
                )
                .hasNext(); 
    }
}

// "diodeIngoingNetConnections"