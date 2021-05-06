package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.containment.ContainmentFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
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
                .property(ContainmentFlag.ASSET_EXISTENCE, !deployed)
                .union(
                    __.identity(),

                    __.out("containedData", "transitData")
                      .where(__.out("containingApp", "transitApp").count().is(1))
                      .property(ContainmentFlag.ASSET_EXISTENCE, !deployed)
                )
                .hasNext();
    }
}