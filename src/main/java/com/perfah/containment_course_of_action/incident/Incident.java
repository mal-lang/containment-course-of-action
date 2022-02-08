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

package com.perfah.containment_course_of_action.incident;

import java.util.Map;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import com.perfah.containment_course_of_action.containment.action.*;
import com.perfah.containment_course_of_action.containment.strategy.*;
import com.perfah.containment_course_of_action.util.Role;
import com.perfah.containment_course_of_action.util.SupportedIntervention;

public abstract class Incident {
    public abstract List<Incident> instantiate(GraphTraversalSource g);
    public abstract String getName();
    public abstract String getDescription(GraphTraversalSource g);
    public abstract String getAttackStepCorrelate();

    public List<ContainmentAction> getContainmentActions2(GraphTraversalSource g){
        Method[] methods = this.getClass().getMethods();

        List<ContainmentAction> interventions = new ArrayList<ContainmentAction>();

        for (Method method: methods) {
            method.setAccessible(true);
            if(method.isAnnotationPresent(SupportedIntervention.class)){
                System.out.println("Supported intervention: " + method.getName());
                try{
                    interventions.addAll(((Stream<ContainmentAction>)method.invoke(this, new Object[]{ g })).collect(Collectors.toList()));
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }

        return interventions;
    }

    public String getInstanceIdentifier(){
        return getName() + getInstanceRoleAssignment();
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
        if(builder.toString().contains(","))
            builder.replace(builder.lastIndexOf(","), builder.length(), "");
        builder.append(")");

        return builder.toString();
    }
}