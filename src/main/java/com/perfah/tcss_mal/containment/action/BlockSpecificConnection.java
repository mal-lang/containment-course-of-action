package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.containment.ContainmentFlag;
import com.perfah.tcss_mal.util.DefenseFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
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
                .property(ContainmentFlag.ASSET_EXISTENCE, !deployed)
                .hasNext();
    }
}
