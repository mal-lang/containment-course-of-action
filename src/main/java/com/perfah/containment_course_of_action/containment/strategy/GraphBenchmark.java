// Copyright 2022 Per Fahlander
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.perfah.containment_course_of_action.containment.strategy;

import core.Attacker;

import java.lang.reflect.Constructor;

//import core.AttackStep;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.perfah.containment_course_of_action.containment.CSAF;
import com.perfah.containment_course_of_action.incident.Incident;
import com.perfah.containment_course_of_action.util.GraphUtil;

import java.util.HashSet;
import java.util.Optional;
import java.util.AbstractMap.SimpleEntry;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.mal_lang.corelang.test.Application;
import org.mal_lang.corelang.test.User;

import core.Asset;
import core.AttackStep;
import core.Defense;

public class GraphBenchmark {
    static List<Class<? extends Asset>> availableAssetTypes = List.of(
        org.mal_lang.corelang.test.Application.class,
        org.mal_lang.corelang.test.ConnectionRule.class,
        org.mal_lang.corelang.test.Credentials.class,
        org.mal_lang.corelang.test.Data.class,
        org.mal_lang.corelang.test.Exploit.class,
        org.mal_lang.corelang.test.Identity.class,
        org.mal_lang.corelang.test.Information.class,
        org.mal_lang.corelang.test.Network.class,
        org.mal_lang.corelang.test.PhysicalZone.class,
        org.mal_lang.corelang.test.RoutingFirewall.class,
        org.mal_lang.corelang.test.SoftwareProduct.class,
        org.mal_lang.corelang.test.System.class,
        org.mal_lang.corelang.test.User.class,
        org.mal_lang.corelang.test.Vulnerability.class,
        org.mal_lang.corelang.test.Group.class
    );

    public String description;
    public HashMap<String, Class<? extends Asset>> assetLookup;
    public HashMap<String, Double> ttcValues = new HashMap<String, Double>();
    public HashMap<String, Boolean> defenseEnabled = new HashMap<String, Boolean>();
    public HashMap<String, List<String>> inhibitedAttackStepsByDefense = new HashMap<String, List<String>>();

    @SuppressWarnings("unchecked")
    public GraphBenchmark(GraphTraversalSource g, String description, List<Incident> activeIncidents, boolean verbose){
        this.description = description;
        if(verbose)
            System.out.println("Benchmarking " + description + "...");

        // Manually clear cached attack steps and defenses for a new instance:
        Asset.allAssets.clear();
        AttackStep.allAttackSteps.clear();
        Defense.allDefenses.clear();
        
        assetLookup = new HashMap<String, Class<? extends Asset>>();

        for(Class<? extends Asset> assetType : availableAssetTypes)
            assetLookup.put(assetType.getSimpleName(), assetType);

        HashMap<Long, Asset> assets = new HashMap<Long, Asset>();
        long attackerId = -1;
        Attacker attacker = new Attacker();

        Map<java.lang.Object, java.lang.Object> rawAssets = g.V()
            .as("asset")
            .select("asset")
            .group()
            .by(T.id)
            .by("metaConcept")            
            .next();
            
        //System.out.println("Available asset types: " + rawAssets.entrySet());
        for (Map.Entry<java.lang.Object, java.lang.Object> rawAsset : rawAssets.entrySet()){
            try {
                
                long assetId = (long)rawAsset.getKey();
                String assetType = (String)((List<java.lang.Object>)rawAsset.getValue()).get(0);

                if(assetType.equalsIgnoreCase("Attacker"))
                {
                    attackerId = assetId;
                }
                else{
                    var modExistence = g.V(assetId).properties(CSAF.ASSET_DETACH);
                    if(modExistence.hasNext() && modExistence.next().value().equals(true)){
                        if(verbose)
                            System.out.println("* Skipping asset: " + GraphUtil.getAssetRefStr(g, assetId));
                        continue;
                    }

                    Asset assetInstance = assetLookup
                        .get(assetType)
                        .getDeclaredConstructor(String.class)
                        .newInstance(assetId + "");
            
                    assets.put(assetId, assetInstance);
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        
        for (Edge edge : g.E().toList()){
            long assocId = (long)edge.id();
            String association = edge.label().split("\\.")[0];

            var modExistence = g.E(assocId).properties(CSAF.ASSOC_DETACH);
            if(modExistence.hasNext() && modExistence.next().value().equals(true))
            {
                if(verbose)
                    System.out.println("* Skipping association: " + GraphUtil.getAssocRefStr(g, assocId));
                continue;
            }

            var modReplacement = g.E(assocId).properties(CSAF.ASSOC_SWAP);
            if(modReplacement.hasNext()) {
                String replacementAssocType = (String)modReplacement.next().value();
                if(!replacementAssocType.equalsIgnoreCase("")){
                    if(verbose)
                        System.out.println("* Replacing association '" + association + "' with '" + replacementAssocType + "' to asset " + GraphUtil.getAssocRefStr(g, assocId));
                    association = replacementAssocType;
                }
            }

            final String assocName = association;

            long inAssetId = (long)edge.inVertex().id();        
            long outAssetId = (long)edge.outVertex().id();
            if(outAssetId == attackerId || !assets.containsKey(outAssetId) || (!assets.containsKey(inAssetId) && inAssetId != attackerId))
                continue;
            
            Asset inAsset = assets.get(inAssetId);
            Asset outAsset = assets.get(outAssetId);
            
            if(inAssetId == attackerId){
                Optional<AttackStep> step = List.of(outAsset.getClass().getFields())
                    .stream()
                    .filter(f -> f.getName().equalsIgnoreCase(assocName))
                    .map(f -> { try { return (AttackStep)f.get(outAsset); } catch(Exception e){ return null; }})
                    .filter(f -> f != null)
                    .findFirst();

                if(step.isPresent()){
                    var incident = activeIncidents
                        .stream()
                        .filter(i -> i.getAttackStepCorrelate().equalsIgnoreCase(step.get().fullName()) || 
                                     i.getAttackStepCorrelate().equalsIgnoreCase("*.*"))
                        .findAny();

                    if(incident.isPresent()){
                        attacker.addAttackPoint(step.get());
                        
                        if(verbose)
                            System.out.println("* Addressing incident '" + incident.get().getName() + "' (coreLang correlate: " + association + ")");
                    }
                }
                else{
                    //System.out.println("Could not add attack point: " + association);
                }
            }
            else{                
                Optional<Field> field = List.of(outAsset.getClass().getFields())
                    .stream()
                    .filter(f -> f.getName().equalsIgnoreCase(assocName))
                    .findFirst();

                if(field.isPresent()){
                    if(field.get().getType().equals(inAsset.getClass())){
                        try{
                            field.get().set(outAsset, inAsset);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    else{
                        try{
                            Set<Asset> set = (Set<Asset>)field.get().get(outAsset);
                            set.add(inAsset);
                            //System.out.println("Successful association: " + association);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                            System.out.println(field.get().getClass());
                            System.out.println(inAsset.getClass());
                            System.out.println("Failed to add association: " + field.get().getName() + " between " + inAsset.name + " and " + outAsset.name);
                        }
                    }
                }
                else{
                    System.out.println("Could not find field for: " + association);
                }
        
            }
        }
        

        for(Defense defense : Defense.allDefenses){
            try{

                int assetId = Integer.parseInt(defense.disable.assetName);
                String defenseName = defense.disable.fullName().split("\\.")[1];

                //System.out.println(defenseName + " = map = " + g.V(assetId).valueMap().next());

                if(!defenseEnabled.containsKey(assetId + "." + defenseName))
                defenseEnabled.put(assetId + "." + defenseName, defense.defaultValue);

                inhibitedAttackStepsByDefense.put(assetId + "." + defenseName, new ArrayList<String>());
            }
            catch(NumberFormatException e){
                e.printStackTrace();
            }
        }

        // The actual calculation of time to compromise:
        //attacker.verbose = true;
        attacker.attack("attackerProfile.ttc");        

        for(AttackStep step : AttackStep.allAttackSteps){
            //System.out.println("AttackStep: TTC of " + step.fullName() + " = " + step.ttc);
            try{
                int assetId = Integer.parseInt(step.fullName().split("\\.")[0]);
                String stepName = assetId + "." + step.attackStepName();
                ttcValues.put(stepName, step.ttc);

                List<String> requiredDisabledDefenses = Stream.concat(step.expectedParents.stream(), 
                                                                      step.visitedParents.stream())
                    .filter(parent -> parent.attackStepName().equalsIgnoreCase("disable"))
                    .map(parent -> parent.assetName + "." + parent.fullName().split("\\.")[1])
                    .collect(Collectors.toList());

                for(String defenseDependency : requiredDisabledDefenses){
                    List<String> attackSteps = inhibitedAttackStepsByDefense.get(defenseDependency);

                    attackSteps.add(assetId + "." + stepName);

                    if(defenseEnabled.get(defenseDependency))
                    {
                        //step.assertUncompromised();
                    }
                }

                if(requiredDisabledDefenses.size() > 0){
                    //System.out.println("Requires disabled defenses: " + requiredDisabledDefenses);
                }
            }
            catch(NumberFormatException e){
                continue;
            }
        }
    }
}
