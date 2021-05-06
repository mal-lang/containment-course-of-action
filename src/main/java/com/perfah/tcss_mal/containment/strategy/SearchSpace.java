package com.perfah.tcss_mal.containment.strategy;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.lang.reflect.*;

import com.perfah.tcss_mal.util.Role;
import com.perfah.tcss_mal.containment.action.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class SearchSpace {
    static List<Class<? extends ContainmentAction>> availableContainmentActions = List.of(
        BlockSpecificConnection.class,
        DropInboundTraffic.class,
        DropOutboundTraffic.class,
        IsolateHost.class,
        //IsolateNetworkSegment.class,
        LockUserAccount.class,
        //RedirectTraffic.class,
        StopService.class,
        SuspendHost.class
    );

    public static List<ContainmentAction> getAllValidContainmentActions(GraphTraversalSource g){
        return availableContainmentActions.stream()
            .map(c -> findPossibleInstances(g, c))
            .flatMap(Collection::stream)
            .filter(ca -> ca.apply(g) && ca.revert(g))
            .collect(Collectors.toList());
    }

    private static<T extends ContainmentAction> List<ContainmentAction> findPossibleInstances(GraphTraversalSource g, Class<T> actionType){
        List<ContainmentAction> actions = new ArrayList<ContainmentAction>();

        try{
            List<String> roleRequirements = new ArrayList<String>();

            for (Field field: actionType.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Role.class)) 
                    roleRequirements.add(field.getDeclaredAnnotation(Role.class).assetType());
            }

            var permutations = findPermutations(g, roleRequirements, new HashMap<String, Long>());

            for(Map<String, Long> permutation : permutations){
                ContainmentAction action = actionType.getDeclaredConstructor(new Class<?>[]{}).newInstance();

                // Assign roles in containment action:
                for(Field field : actionType.getDeclaredFields()){
                    if(field.isAnnotationPresent(Role.class)){                        
                        field.set(action, permutation.get(field.getDeclaredAnnotation(Role.class).assetType()));
                    }                    
                }

                actions.add(action);
            }
        
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
        return actions;
    }

    private static List<Map<String, Long>> findPermutations(GraphTraversalSource g,
                                                            List<String> roleRequirements, 
                                                            Map<String, Long> rolesAssignments){

        if(rolesAssignments.size() == roleRequirements.size()){
            return List.of(Map.copyOf(rolesAssignments));
        }                                                                                
        else{
            return g.V()
                .has("metaConcept", roleRequirements.get(rolesAssignments.size()))
                .id()
                .toStream()
                .map(id -> (long)id)
                .map(id -> {
                    rolesAssignments.put(roleRequirements.get(rolesAssignments.size()), id);
                    List<Map<String, Long>> instances = findPermutations(g, roleRequirements, rolesAssignments);
                    rolesAssignments.remove(roleRequirements.get(rolesAssignments.size() - 1));
                    return instances;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        }
    }
}
