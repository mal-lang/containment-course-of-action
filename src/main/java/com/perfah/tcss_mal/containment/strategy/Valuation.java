package com.perfah.tcss_mal.containment.strategy;

import java.io.BufferedReader;
import java.util.HashMap;

import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Sugar;

import java.io.InputStreamReader;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class Valuation {
    private HashMap<String, Double> cod;
    private HashMap<String, Double> coa;
    private BufferedReader reader;
    private GraphTraversalSource g;

    public Valuation(GraphTraversalSource g) {
        cod = new HashMap<String, Double>();
        coa = new HashMap<String, Double>();
        reader = new BufferedReader(new InputStreamReader(System.in));
        this.g = g;
    }

    public double getContainmentActionConsequence(String containmentActionInstance){

        // if(containmentActionInstance.contains("DropOutgoingTraffic"))
        // {
        //     System.out.println("eehhh");
        // }

        if(cod.containsKey(containmentActionInstance))
            return cod.get(containmentActionInstance);
        else {
            while(true){
                try{
                    System.out.print("-> Estimate cost of deploying containment action '" + 
                        Sugar.ANSI_BOLD + 
                        containmentActionInstance + 
                        Sugar.ANSI_RESET +
                        "': ");
                    double value = Double.parseDouble(reader.readLine());
                    cod.put(containmentActionInstance, value);
                    return value;
                }
                catch(Exception e){
                    continue;
                }
            }
        }
    }

    public double getAttackStepConsequence(String step){
        if(coa.containsKey(step))
            return coa.get(step);
        else {
            try{
                String[] arr = step.split("\\.");
                System.out.print("-> Estimate cost if attack step '" + Sugar.ANSI_BOLD + GraphUtil.getAttackStepRefStr(g, Long.parseLong(arr[0]), arr[1]) + Sugar.ANSI_RESET + "' is compromised (defaults to 0): ");
                double value = Double.parseDouble(reader.readLine());
                coa.put(step, value);
                return value;
            }
            catch(Exception e){
                double defaultVal = 0.0;
                coa.put(step, defaultVal);
                return defaultVal;
            }          
        }
    }

    public void addCostOfDefense(long assetId, String defenseName, double cost) {
 
    }

    public void addCostOfCompromise(long assetId, String attackStepName, double cost) {

    }
}
