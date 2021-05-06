package com.perfah.tcss_mal.containment.action;

import java.lang.reflect.*;
import java.util.List;

import com.perfah.tcss_mal.containment.evaluation.GraphBenchmark;
import com.perfah.tcss_mal.incident.Incident;
import com.perfah.tcss_mal.util.Role;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public abstract class ContainmentAction  {
    private boolean active;

    public ContainmentAction(){
        active = false;
    }

    public abstract String getName();
    public abstract String getDescription(GraphTraversalSource g);
    protected abstract boolean setDeployed(GraphTraversalSource g, boolean deployed);


    public boolean getActive(){
        return active;
    }

    public boolean apply(GraphTraversalSource g){
        active = setDeployed(g, true);
        return active;
    }
    public boolean revert(GraphTraversalSource g){
        active = !setDeployed(g, false);
        return !active;
    }

    public GraphBenchmark benchmark(GraphTraversalSource g, List<Incident> activeIncidents){
        if(active){
            System.out.println("Warning: " + getInstanceIdentifier() + " is already applied.");
        }
        
        if(!apply(g)){
            System.out.println(getInstanceIdentifier() + " is not applicable");
            return null;
        }
        
        GraphBenchmark benchmark = new GraphBenchmark(g, getInstanceIdentifier(), activeIncidents, true);

        if(!revert(g)){
            System.out.println("Failed to revert: " + getInstanceIdentifier());
            return null;
        }

        return benchmark;
    } 

    public String getInstanceIdentifier(){
        return getName() + getInstanceRoleAssignment();
    }

    @Override
    public String toString(){
        return getInstanceIdentifier();
    }

    private String getInstanceRoleAssignment() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");

        Field[] roleFields = this.getClass().getDeclaredFields();
        for (Field field: roleFields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Role.class)) {
                try {
                    builder.append(field.getName() + "=" + field.getLong(this) + ",");
                }
                catch(IllegalAccessException e){}
            }
        }
        builder.replace(builder.lastIndexOf(","), builder.length(), "");
        builder.append(")");

        return builder.toString();
    }
}
