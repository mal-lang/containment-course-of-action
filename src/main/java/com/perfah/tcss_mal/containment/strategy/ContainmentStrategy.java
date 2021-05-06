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

    public ContainmentStrategy(List<ContainmentAction> activeActions, List<ContainmentAction> latentActions, Random rand){
        this.activeActions = new ArrayList<ContainmentAction>(activeActions);
        this.latentActions = new ArrayList<ContainmentAction>(latentActions);
        this.rand = rand;
    }

    public ContainmentStrategy(List<ContainmentAction> actions){
        activeActions = new ArrayList<ContainmentAction>();
        latentActions = new ArrayList<ContainmentAction>(actions);
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

    public ContainmentStrategy neighbor(GraphTraversalSource g, ContainmentStrategy bestStrategyYet){
        if(activeActions.size() + latentActions.size() == 0)
            return null;

        if(rand.nextInt(MAX_SIZE + 1) != 0){
            // Probability of entering: n/(n+1)

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
                return neighbor(g, Operation.INSERT, activeIndex, latentIndex);
            }
            else if(!activeActions.isEmpty() && latentActions.isEmpty()){
                return neighbor(g, Operation.REMOVE, activeIndex, latentIndex);
            }
            else if(activeActions.size() >= MAX_SIZE){
                int k = rand.nextInt(2);
                if(k == 0){
                    return neighbor(g, Operation.REMOVE, activeIndex, latentIndex);
                }
                else {
                    return neighbor(g, Operation.REPLACE, activeIndex, latentIndex);
                }
            }
            else {
                // RANDOMIZE
                int k = rand.nextInt(3);
                if(k == 0){
                    return neighbor(g, Operation.INSERT, activeIndex, latentIndex);
                }
                else if(k == 1){
                    return neighbor(g, Operation.REMOVE, activeIndex, latentIndex);
                }
                else {
                    return neighbor(g, Operation.REPLACE, activeIndex, latentIndex);
                }
            }
        }
        else{
            // Avoid getting stuck by returning to the best configuration yet seen
            // Probability of entering: 1/(n+1)
            return bestStrategyYet;
        }
    }

    public ContainmentStrategy neighbor(GraphTraversalSource g, Operation op, int activeIndex, int latentIndex){
        ContainmentStrategy neighbor = new ContainmentStrategy(activeActions, latentActions, rand);
        
        if(op == Operation.INSERT){
            ContainmentAction a = neighbor.latentActions.remove(latentIndex);
            neighbor.activeActions.add(activeIndex, a);
        }
        else if (op == Operation.REMOVE){
            ContainmentAction a = neighbor.activeActions.remove(activeIndex);
            neighbor.latentActions.add(latentIndex, a);
        }
        else if (op == Operation.REPLACE){
            ContainmentAction a = neighbor.activeActions.get(activeIndex);
            ContainmentAction b = neighbor.latentActions.remove(latentIndex);            

            neighbor.activeActions.set(activeIndex, b);
            neighbor.latentActions.add(latentIndex, a);
        }

        return neighbor;
    }
 
}
