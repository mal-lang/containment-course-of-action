package com.perfah.tcss_mal.containment.action;

import com.perfah.tcss_mal.containment.ContainmentFlag;
import com.perfah.tcss_mal.util.DefenseFlag;
import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Role;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;

public class LockUserAccount extends ContainmentAction {
    @Role(description = "An account owned by the user.", assetType = "Identity")
    public long account;

    public LockUserAccount(){}

    public LockUserAccount(long account){
        this.account = account;
    }

    @Override
    public String getName(){
        return "LockUserAccount";
    }

    @Override
    public String getDescription(GraphTraversalSource g) {
        return String.format(
            "Lock user account %s and associated credentials.", 
            GraphUtil.getAssetRefStr(g, account));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean setDeployed(GraphTraversalSource g, boolean deployed) {
        return 
            g.V(account)
                .has("metaConcept", "Identity")
                .union(
                    __.property(ContainmentFlag.ASSET_EXISTENCE, !deployed),

                    __.out("credentials")
                      .property(ContainmentFlag.ASSET_EXISTENCE, !deployed)
                )
                .hasNext();
    }
}

