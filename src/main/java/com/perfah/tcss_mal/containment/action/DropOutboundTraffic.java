package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.containment.ContainmentFlag;
import com.perfah.tcss_mal.util.DefenseFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
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
                      .property(ContainmentFlag.ASSOC_REPLACEMENT, deployed ? "ingoingAppConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("applications", "outApplications")
                      .filter(__.inV().hasId(sender))
                      .property(ContainmentFlag.ASSOC_REPLACEMENT, deployed ? "inApplications" : ""),

                    __.outE("netConnections", "outgoingNetConnections")
                      .property(ContainmentFlag.ASSOC_REPLACEMENT, deployed ? "ingoingNetConnections" : "")
                      .inV()
                      .has("metaConcept", "ConnectionRule")
                      .outE("networks", "outNetworks", "diodeOutNetworks")
                      .filter(__.inV().hasId(sender))
                      .property(ContainmentFlag.ASSOC_REPLACEMENT, deployed ? "inNetworks" : "")
                )
                .hasNext();           
    }
}
