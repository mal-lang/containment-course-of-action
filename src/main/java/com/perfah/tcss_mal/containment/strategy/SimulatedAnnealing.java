package com.perfah.tcss_mal.containment.strategy;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.perfah.tcss_mal.containment.action.BlockSpecificConnection;
import com.perfah.tcss_mal.containment.action.ContainmentAction;
import com.perfah.tcss_mal.incident.Incident;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class SimulatedAnnealing {
    // Reference: startingTemp = 10, iterations = 10000, 0.9995

    final double initialTemperature = 10000000.0;
    final double coolingRate = 0.98;
    final int maxIterations = 1000;

    private int currentIteration;
    private ContainmentStrategy acceptedStrategy;
    private ContainmentStrategy bestStrategyYet;
    private double t;
    private GraphBenchmark initialBenchmark;
    private Valuation valuation;
    private List<Incident> activeIncidents;
    private List<ContainmentAction> latentActions;
    private double bestEnergy;

    private double proposedHarmReduction, acceptedHarmReduction;
    private double proposedContainmentCost, acceptedContainmentCost;
    private double acceptedEnergy;
 
    private ArrayList<ArrayList<Object>> rows;

    private final String format = "%1$-10s %2$-15s %3$-50s %4$-50s %5$-50s %6$-40s %7$-40s %8$-40s\n";

    public SimulatedAnnealing(GraphTraversalSource g,
                              List<Incident> activeIncidents,
                              Valuation valuation,
                              List<ContainmentAction> containmentActions) {   
        this.activeIncidents = activeIncidents;
        this.latentActions = new ArrayList<ContainmentAction>(containmentActions);

        currentIteration = 0;
        acceptedStrategy = new ContainmentStrategy(latentActions);
        bestStrategyYet = acceptedStrategy;
        t = initialTemperature;
        bestEnergy = 0;

        this.initialBenchmark = new GraphBenchmark(g, "Initial", activeIncidents, true);
        this.valuation = valuation;
        this.rows = new ArrayList<ArrayList<Object>>();
    }

    public void execute(GraphTraversalSource g) {
        ArrayList<Object> header = new ArrayList<Object>();
        header.add("It.");
        header.add("Temperature");
        for(int i = 0; i < ContainmentStrategy.MAX_SIZE; i++)
            header.add("Action No. " + (i+1));
        header.add("Harm reduction");
        header.add("Containment cost");
        header.add("Rating");
        rows.add(header);

        // Enforce containment action valuations in the beginning:
        latentActions.stream().forEach(action -> valuation.getContainmentActionConsequence(action.getInstanceIdentifier()));

        while(t > 1 && currentIteration++ < 999999999){
            ContainmentStrategy backup = acceptedStrategy;
            acceptedStrategy = acceptedStrategy.neighbor(g, bestStrategyYet);
            if(acceptedStrategy == null)
                return;
            
            double neighborEnergy = energy(g);
            if(neighborEnergy > bestEnergy){
                // Accept
                bestEnergy = neighborEnergy;
                acceptedHarmReduction = proposedHarmReduction;
                acceptedContainmentCost = proposedContainmentCost;
                acceptedEnergy = bestEnergy;
                bestStrategyYet = acceptedStrategy;
            }
            else if(Math.exp((neighborEnergy - bestEnergy) / t) < Math.random()){
                // Deny
                acceptedStrategy = backup;
            }
            else {
                // Accept
                acceptedHarmReduction = proposedHarmReduction;
                acceptedContainmentCost = proposedContainmentCost;
                acceptedEnergy = neighborEnergy;
            }


            //System.out.println("x = " + Math.exp((bestEnergy - neighborEnergy) / t)) ;
            //System.out.println("t = " + t);
            t *= coolingRate;
            
            // Add row:

            ArrayList<Object> row = new ArrayList<Object>();
            row.add(currentIteration);
            row.add((int)t);
            for(int i = 0; i < ContainmentStrategy.MAX_SIZE; i++){
                if(i < acceptedStrategy.getContainmentActions().size())
                    row.add(acceptedStrategy.getContainmentActions().get(i).getInstanceIdentifier());
                else
                    row.add("NOP");
            }
            row.add(acceptedHarmReduction);
            row.add(acceptedContainmentCost);
            row.add(acceptedEnergy);
            rows.add(row);
        
        }
    }

    public void printProcess(){
        System.out.println();
        System.out.format(format, rows.get(0).toArray());
        System.out.println();
        for(ArrayList<Object> row : rows.subList(1, rows.size()))
            System.out.format(format, row.toArray());
    }

    public void printResults(GraphTraversalSource g){
        List<ContainmentAction> bestActions = bestStrategyYet.getContainmentActions();
        
        if(bestActions.size() > 0){
            System.out.println("Recommended containment strategy for indicated incidents:\n");
            for(int i = 0; i < bestActions.size(); i++){
                System.out.println((i+1) + ". " + bestActions.get(i).getInstanceIdentifier() + " - " + bestActions.get(i).getDescription(g));
            }
        }
        else {
            System.out.println("Suggested containment strategy: Do nothing!");
        }
    }

    public double energy(GraphTraversalSource g){
        //System.out.println("Testing strategy: " + strategy);

        GraphBenchmark benchmark = acceptedStrategy.benchmark(g, activeIncidents);

        double harmReduction = 0.0;
        for(String attackStep : initialBenchmark.ttcValues.keySet()) {
            double preTTC = initialBenchmark.ttcValues.get(attackStep);
            double postTTC;
            if(benchmark.ttcValues.containsKey(attackStep))
                postTTC = benchmark.ttcValues.get(attackStep);
            else
                postTTC = Double.MAX_VALUE;

            double diff = (UAF(preTTC) - UAF(postTTC));
            if(diff == 0.0)
                continue;

            //System.out.println(attackStep + ": " + preTTC + " -> " + postTTC);

            double consequence = valuation.getAttackStepConsequence(attackStep);
            double tmp = consequence * (UAF(preTTC) - UAF(postTTC));
            harmReduction += tmp;

            if(tmp < 0.0){
                System.out.println("!");
                System.out.println("Strategy: " + acceptedStrategy.toString());
                System.out.println("Attack step" + attackStep);
                System.out.println(preTTC + " -> " + postTTC);
                System.out.println("Consequence: " + consequence);
            }
        }

        double containmentCost = 0.0;
        for(ContainmentAction action : acceptedStrategy.getContainmentActions()){
            containmentCost += valuation
                .getContainmentActionConsequence(action.getInstanceIdentifier());
        }

        proposedHarmReduction = harmReduction;
        proposedContainmentCost = containmentCost;

        double result;
        if (harmReduction > 0.0 && containmentCost == 0.0){
            result = harmReduction;
        }
        else if(containmentCost == 0.0)
            result = 1.0;
        else{
            result = harmReduction / containmentCost;
        }

        return result;
    }

    public double UAF(double ttc) {
        return 1.0 - ttc / (ttc + 1);
    }
}
