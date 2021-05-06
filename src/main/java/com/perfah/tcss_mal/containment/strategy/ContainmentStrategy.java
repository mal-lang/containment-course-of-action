package com.perfah.tcss_mal.containment.strategy;

import java.util.Collections;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import com.perfah.tcss_mal.containment.action.ContainmentAction;
import com.perfah.tcss_mal.incident.Incident;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class ContainmentStrategy {
    public static final int MAX_SIZE = 3;
    private List<ContainmentAction> activeActions;
    private List<ContainmentAction> latentActions;
    private Random rand;

    public enum Operation {
        INSERT,
        REMOVE,
        REPLACE
    }

    public ContainmentStrategy(List<ContainmentAction> actions){
        activeActions = new ArrayList<ContainmentAction>();
        latentActions = actions;
        rand = new Random();
    }

    public List<ContainmentAction> getContainmentActions(){
        return activeActions;
    }

    @Override
    public String toString(){
        return activeActions.toString();
    }

    public GraphBenchmark benchmark(GraphTraversalSource g, List<Incident> activeIncidents){
        for(ContainmentAction action : activeActions)
            action.apply(g);

        GraphBenchmark benchmark = new GraphBenchmark(g, toString(), activeIncidents, false);

        for(int i = activeActions.size() - 1; i >= 0; i--)
            activeActions.get(i).revert(g);

        return benchmark;
    }

    public Neighbor neighbor(){
        if(activeActions.size() + latentActions.size() == 0)
            return null;

        int activeIndex;
        if(activeActions.size() > 1)
            activeIndex = rand.nextInt(activeActions.size() - 1);
        else
            activeIndex = 0;

        int latentIndex;
        if(latentActions.size() > 1)
            latentIndex = rand.nextInt(latentActions.size() - 1);
        else
            latentIndex = 0;

        if (activeActions.isEmpty() && !latentActions.isEmpty()){
            return new Neighbor(Operation.INSERT, activeIndex, latentIndex);
        }
        else if(!activeActions.isEmpty() && latentActions.isEmpty()){
            return new Neighbor(Operation.REMOVE, activeIndex, latentIndex);
        }
        else if(activeActions.size() >= MAX_SIZE){
            int k = rand.nextInt(2);
            if(k == 0){
                return new Neighbor(Operation.REMOVE, activeIndex, latentIndex);
            }
            else {
                return new Neighbor(Operation.REPLACE, activeIndex, latentIndex);
            }
        }
        else {
            // RANDOMIZE
            int k = rand.nextInt(3);
            if(k == 0){
                return new Neighbor(Operation.INSERT, activeIndex, latentIndex);
            }
            else if(k == 1){
                return new Neighbor(Operation.REMOVE, activeIndex, latentIndex);
            }
            else {
                return new Neighbor(Operation.REPLACE, activeIndex, latentIndex);
            }
        }
    }

    public class Neighbor{
        final Operation op;
        final int activeIndex, latentIndex;

        public Neighbor(Operation op, int activeIndex, int latentIndex){
            this.op = op;
            this.activeIndex = activeIndex;
            this.latentIndex = latentIndex;
        }

        public void moveTo(GraphTraversalSource g){
            modify(g, op, false);
        }

        public void moveBack(GraphTraversalSource g){
            modify(g, op, true);
        }

        private void modify(GraphTraversalSource g, Operation op, boolean revert){
            if((op == Operation.INSERT && !revert) || (op == Operation.REMOVE && revert)){
                ContainmentAction a = latentActions.remove(latentIndex);
                activeActions.add(activeIndex, a);
            }
            else if ((op == Operation.REMOVE && !revert) || (op == Operation.INSERT && revert)){
                ContainmentAction a = activeActions.remove(activeIndex);
                latentActions.add(latentIndex, a);
            }
            else if (op == Operation.REPLACE){
                ContainmentAction a = activeActions.get(activeIndex);
                ContainmentAction b = latentActions.remove(latentIndex);            
    
                activeActions.set(activeIndex, b);
                latentActions.add(latentIndex, a);
            }
        }
    }
}
