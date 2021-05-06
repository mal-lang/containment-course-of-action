package com.perfah.tcss_mal.containment.evaluation;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.perfah.tcss_mal.containment.action.BlockSpecificConnection;
import com.perfah.tcss_mal.containment.action.ContainmentAction;
import com.perfah.tcss_mal.containment.strategy.ContainmentStrategy;
import com.perfah.tcss_mal.incident.Incident;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class SimulatedAnnealing {
    // Reference: startingTemp = 10, iterations = 10000, 0.9995

    final double initialTemperature = 40000.0;
    final double coolingRate = 0.99;
    final int maxIterations = 1000;

    private int currentIteration;
    private ContainmentStrategy strategy;
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

    private final String format = "%1$-10s %2$-50s %3$-50s %4$-50s %5$-40s %6$-40s %7$-40s\n";

    public SimulatedAnnealing(GraphTraversalSource g,
                              List<Incident> activeIncidents,
                              Valuation valuation,
                              List<ContainmentAction> containmentActions) {   
        this.activeIncidents = activeIncidents;
        this.latentActions = new ArrayList<ContainmentAction>(containmentActions);

        currentIteration = 0;
        strategy = new ContainmentStrategy(latentActions);
        t = initialTemperature;
        bestEnergy = 0;

        this.initialBenchmark = new GraphBenchmark(g, "Initial", activeIncidents, true);
        this.valuation = valuation;
        this.rows = new ArrayList<ArrayList<Object>>();
    }

    public void execute(GraphTraversalSource g) {
        ArrayList<Object> header = new ArrayList<Object>();
        header.add("It.");
        for(int i = 0; i < ContainmentStrategy.MAX_SIZE; i++)
            header.add("Action No. " + (i+1));
        header.add("Harm reduction");
        header.add("Containment cost");
        header.add("Rating");
        rows.add(header);

        // Enforce containment action valuations in the beginning:
        latentActions.stream().forEach(action -> valuation.getContainmentActionConsequence(action.getInstanceIdentifier()));

        while(t > 0.5 && currentIteration++ < 999999999){
            ContainmentStrategy.Neighbor neighbor = strategy.neighbor();
            if(neighbor == null)
                return;

            neighbor.moveTo(g);
            
            double neighborEnergy = energy(g);
            if(neighborEnergy > bestEnergy){
                // Accept
                bestEnergy = neighborEnergy;
                acceptedHarmReduction = proposedHarmReduction;
                acceptedContainmentCost = proposedContainmentCost;
                acceptedEnergy = bestEnergy;
            }
            else if(Math.exp((neighborEnergy - bestEnergy) / t) < Math.random()){
                // Deny
                neighbor.moveBack(g);
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
            for(int i = 0; i < ContainmentStrategy.MAX_SIZE; i++){
                if(i < strategy.getContainmentActions().size())
                    row.add(strategy.getContainmentActions().get(i).getInstanceIdentifier());
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
        List<ContainmentAction> bestActions = strategy.getContainmentActions();
        
        if(bestActions.size() > 0){
            System.out.println("\nPreferred containment strategy (perform actions chronologically):\n");
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

        GraphBenchmark benchmark = strategy.benchmark(g, activeIncidents);

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
            harmReduction += consequence * (UAF(preTTC) - UAF(postTTC));
        }

        double containmentCost = 0.0;
        for(ContainmentAction action : strategy.getContainmentActions()){
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
