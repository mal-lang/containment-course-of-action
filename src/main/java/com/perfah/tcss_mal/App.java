package com.perfah.tcss_mal;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.ListIterator;
import java.util.Optional;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.log4j.LogManager;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Client.SessionedClient;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteTraversal;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.util.Gremlin;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.finalization.ReferenceElementStrategy;

import com.perfah.tcss_mal.util.GraphUtil;
import com.perfah.tcss_mal.util.Sugar;
import com.perfah.tcss_mal.containment.action.*;
import com.perfah.tcss_mal.containment.strategy.*;
import com.perfah.tcss_mal.incident.*;

public class App 
{
    private List<Incident> latentIncidents;
    private List<Incident> activeIncidents;
    private List<ContainmentAction> containmentActions;
    private GraphTraversalSource g;

    public App() {     
/*         g = AnonymousTraversalSource
            .traversal()
            .withRemote(DriverRemoteConnection.using("localhost", 8182)); */
    
        Cluster cluster = Cluster.build()
            .addContactPoint("localhost")
            .port(8182)
            .create();

        g = AnonymousTraversalSource
            .traversal()
            .withRemote(DriverRemoteConnection.using(cluster));

        latentIncidents = Stream
            .of(new Phishing(), new EndpointDenialOfService(), new AnyIncident())
            .map(factory -> factory.instantiate(g))
            .flatMap(List::stream)
            .collect(Collectors.toList());

        activeIncidents = new ArrayList<Incident>();

        containmentActions = SearchSpace.getAllValidContainmentActions(g);
    }

    public void userAction() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));   

        System.out.println();
        System.out.println("~ Containment Strategy Prototype ~ ");
        System.out.println(Sugar.ANSI_BOLD + "Enter a command (type 'help' to list all commands): " + Sugar.ANSI_RESET);
        System.out.print(Sugar.ANSI_BOLD + ">" + "\033[00" + Sugar.ANSI_RESET + " ");
        
        try {
            String[] command = reader
                                .readLine()
                                .split("\\ ");

            switch(command[0]){
                case "indicate": case "i":
                    if(command.length < 2){
                        System.out.println("Invalid args.");
                        break;
                    }

                    Optional<Incident> incident = IntStream.range(0, latentIncidents.size())
                        .filter(i -> command[1].equalsIgnoreCase((i+1) + "") || latentIncidents.get(i).getInstanceIdentifier().equalsIgnoreCase(command[1]))
                        .mapToObj(i -> latentIncidents.get(i))
                        .findFirst();

                    if(incident.isPresent()){
                        if(activeIncidents.contains(incident.get())){
                            activeIncidents.remove(incident.get());
                            System.out.println(incident.get().getInstanceIdentifier() + " marked as latent.");
                        }
                        else{
                            activeIncidents.add(incident.get());
                            System.out.println(incident.get().getInstanceIdentifier() + " marked as indicated.");
                        }
                    }
                    else{
                        System.out.println("Could not find incident " + command[1]);
                    }

                    break;
                case "tentative": case "threats": case "t":
                    System.out.println("Tentative incidents/threats:\n");
                    
                    String incidentFormat = "%1$-5s %2$-70s %3$-20s %4$-150s\n";
                    System.out.format(incidentFormat, Stream.of("N", "ID", "Status", "Description").toArray());
                    for(int i = 0; i < latentIncidents.size(); i++){
                        System.out.format(String.format(incidentFormat, new Object[] {
                            i+1, 
                            latentIncidents.get(i).getInstanceIdentifier(), 
                            activeIncidents.contains(latentIncidents.get(i)) ? "Indicated" : "Latent",
                            latentIncidents.get(i).getDescription(g)
                        }));
                    }
                    break;
                case "actions": case "a":
                    System.out.println("Viable containment actions:\n");

                    String actionFormat = "%1$-5s %2$-50s %3$-150s \n";
                    System.out.format(actionFormat, Stream.of("N", "ID", "Description").toArray());
                    for(int i = 0; i < containmentActions.size(); i++){
                        System.out.format(String.format(actionFormat, new Object[] {
                            i+1, 
                            containmentActions.get(i).getInstanceIdentifier(), 
                            containmentActions.get(i).getDescription(g)
                        }));
                    }
                    break;
                case "benchmark": case "b":
                    if(command.length < 2){
                        System.out.println("Invalid args.");
                        break;
                    }

                    Optional<ContainmentAction> action = IntStream.range(0, containmentActions.size())
                        .filter(i -> command[1].equalsIgnoreCase((i+1) + "") || containmentActions.get(i).getInstanceIdentifier().equalsIgnoreCase(command[1]))
                        .mapToObj(i -> containmentActions.get(i))
                        .findFirst();

                    if(action.isPresent()){
                        GraphBenchmark initialBenchmark = new GraphBenchmark(g, "Initial", activeIncidents, true);
                        GraphBenchmark benchmark = action.get().benchmark(g, activeIncidents);

                        String format = "%1$-50s %2$-50s %3$-50s %4$-50s \n";
                        String tableFormat = "%s\t%s\t%s\t%s\n";
                        boolean table = command.length == 3 && command[2].equalsIgnoreCase("tsv");

                        System.out.format(table ? tableFormat : format, Stream.of("Attack step", "Pre-TTC", "Post-TTC", "Protected?").toArray());

                        initialBenchmark
                            .ttcValues
                            .entrySet()
                            .stream()
                            .map(e -> new Object[]{ 
                                GraphUtil.getAssetRefStr(g, Long.parseLong(e.getKey().split("\\.")[0])) + "." + e.getKey().split("\\.")[1],
                                Sugar.formatInfinity(e.getValue().doubleValue()),
                                Sugar.formatInfinity(benchmark.ttcValues.getOrDefault(e.getKey(), Double.MAX_VALUE)),
                                e.getValue().doubleValue() != benchmark.ttcValues.getOrDefault(e.getKey(), Double.MAX_VALUE) ? (table ? "POS" : "YES") : (table ? "NEG" : "")
                            })
                            .sorted((a, b) -> a[0].toString().compareTo(b[0].toString()))
                            .forEach(str -> System.out.format(table ? tableFormat : format, str));
                    }
                    else{
                        System.out.println("Could not find containment action " + command[1]);
                    }

                    break;
                case "strategy": case "s":
                    Valuation valuation = new Valuation(g);
                    SimulatedAnnealing sa = new SimulatedAnnealing(g, activeIncidents, valuation, containmentActions);
                    sa.execute(g);
                    sa.printProcess();
                    sa.printResults(g);
                    break;
                case "names":
                    g.V()
                        .as("asset")
                        .select("asset")
                        .group()
                        .by(T.id)
                        .by("name")            
                        .toStream()
                        .forEach(x -> System.out.println(x));
                    break;
                case "help": case "h":
                    System.out.println("Available commands: ");
                    System.out.println("- tentative/threats/t: List all indicated and latent incidents");
                    System.out.println("- indicate/i <incident/incident-id>: Toggle a specific incident as indicatated");
                    System.out.println("- benchmark/b <action/action-id>: Benchmark a specific containment action");
                    System.out.println("- strategy/s: Find the best strategy");
                    System.out.println("- help/h: This help");
                    System.out.println("- quit: Revert all and quit the program");
                    break;
                default:
                    break;
            }
        }
        catch(Exception e){
            System.out.println("Invalid command!");
            e.printStackTrace();
        }
    }

    public static void main( String[] args )
    {   
        System.err.close();
        System.setErr(System.out);
        LogManager.shutdown();

        try {       
            App app = new App();

            while(true) {
                app.userAction();
            }
        }
        catch(Exception e){
            System.out.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        finally{

        }
    }
}