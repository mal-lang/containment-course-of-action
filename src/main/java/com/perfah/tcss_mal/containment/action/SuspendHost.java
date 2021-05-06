package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.containment.ContainmentFlag;
import com.perfah.tcss_mal.util.DefenseFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
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
                .property(ContainmentFlag.ASSET_EXISTENCE, !deployed)
                .union(
                    __.out("sysData")
                      .property(ContainmentFlag.ASSET_EXISTENCE, !deployed),

                    __.repeat(__.out("sysExecutedApps")).until(__.out("sysExecutedApps").count().is(0))
                      .union(
                        __.identity(),
                        __.repeat(__.out("appExecutedApps")).until(__.out("appExecutedApps").count().is(0))
                      )
                      .has("metaConcept", "Application")
                      .property(ContainmentFlag.ASSET_EXISTENCE, !deployed)
                      .union(
                        __.identity(),
    
                        __.out("containedData", "transitData")
                          .where(__.out("containingApp", "transitApp").count().is(1))
                          .property(ContainmentFlag.ASSET_EXISTENCE, !deployed)
                      )
                )
                .hasNext();
    }
}
