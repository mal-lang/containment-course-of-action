package com.perfah.tcss_mal.util;

import java.security.InvalidParameterException;
import java.util.HashMap;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class GraphUtil {
    public static String getAssetRefStr(GraphTraversalSource g, long assetId){
        var label = g.V(assetId).label();
        return label.hasNext() ? label.next().split("\\-")[0] : assetId + "";
        //return label.hasNext() ? assetId + "/" + label.next() : assetId + "";
    }

    public static String getAttackStepRefStr(GraphTraversalSource g, long assetId, String attackStepName){
        return getAssetRefStr(g, assetId) + "." + attackStepName;
    }

    public static String getAssocRefStr(GraphTraversalSource g, long assocId){
        return getAssetRefStr(g, (long)g.E(assocId).outV().id().next()) + "[" + g.E(assocId).label().next() + "] = " + getAssetRefStr(g, (long)g.E(assocId).inV().id().next());
    }

    // 
}
