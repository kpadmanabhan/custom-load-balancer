package main;

import main.loadbalancer.LoadBalancer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class acts as the main entry point as well as the client hitting the load balancer
 */
public class CustomLoadBalancer {
    private static LoadBalancer loadBalancer;

    public static void main(String[] args) {
        loadBalancer = LoadBalancer.getInstance();

        // Step 2 – Register a list of providers
        System.out.println("-------------------------------------");
        System.out.println("Step 2 – Register a list of providers");
        System.out.println("-------------------------------------");
        try {
            // we do not expect an exception here as we are registering only 8 providers whereas max capacity is 10
            registerProviders(8);
        } catch (LoadBalancerException e) {
            System.err.println(e.getMessage());
        }

        // Step 3 – Random invocation
        System.out.println("-------------------------------------");
        System.out.println("Step 3 – Random invocation");
        System.out.println("-------------------------------------");
        try {
            randomInvocation(100);
        } catch (LoadBalancerException e) {
            System.err.println(e.getMessage());
        }

        // Step 4 – Round Robin invocation
        System.out.println("-------------------------------------");
        System.out.println("Step 4 – Round Robin invocation");
        System.out.println("-------------------------------------");
        try {
            roundRobinInvocation(100);
        } catch (LoadBalancerException e) {
            System.err.println(e.getMessage());
        }

        // Step 5 – Manual node exclusion / inclusion
        System.out.println("-------------------------------------");
        System.out.println("Step 5 – Manual node exclusion / inclusion");
        System.out.println("-------------------------------------");
        try {
            includeAndExcludeProvider();
        } catch (LoadBalancerException e) {
            System.err.println(e.getMessage());
        }

        // Step 6 – Heart beat checker
        System.out.println("-------------------------------------");
        System.out.println("Step 6 – Heart beat checker");
        System.out.println("-------------------------------------");
        try {
            heartbeat();
        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        }

        // Step 7 – Improving Heart beat checker
        System.out.println("-------------------------------------");
        System.out.println("Step 7 – Improving Heart beat checker");
        System.out.println("-------------------------------------");
        try {
            optimizedHeartbeat();
        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        }

        // Step 8 – Cluster Capacity Limit
        System.out.println("-------------------------------------");
        System.out.println("Step 8 – Cluster Capacity Limit");
        System.out.println("-------------------------------------");
        try {
            invokeParallelRequests();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static void registerProviders(int num) throws LoadBalancerException {
        System.out.println("Registering " + num + " providers...");
        loadBalancer.registerProviders(num);
    }

    private static void randomInvocation(int times) throws LoadBalancerException {
        for (int i = 0; i < times; i++) {
            System.out.println("Random invoke from loadbalancer : " + loadBalancer.get(InvocationPattern.RANDOM));
        }
    }

    private static void roundRobinInvocation(int times) throws LoadBalancerException {
        for (int i = 0; i < times; i++) {
            System.out.println("Round-robin invoke from loadbalancer : " + loadBalancer.get(InvocationPattern.ROUND_ROBIN));
        }
    }

    private static void includeAndExcludeProvider() throws LoadBalancerException {
        System.out.println("Active provider count initial: " + loadBalancer.getActiveProviderKeys().size());

        // exclude first provider
        var provider1 = loadBalancer.getActiveProviderKeys().stream().findFirst().orElse(null);
        if (provider1 != null) {
            System.out.println("Excluding provider: " + provider1);
            loadBalancer.excludeProvider(provider1);
        }
        System.out.println("Active provider count after exclude: " + loadBalancer.getActiveProviderKeys().size());

        // exclude another provider
        var provider2 = loadBalancer.getActiveProviderKeys().stream().findFirst().orElse(null);
        if (provider2 != null) {
            System.out.println("Excluding provider: " + provider2);
            loadBalancer.excludeProvider(provider2);
        }
        System.out.println("Active provider count after exclude: " + loadBalancer.getActiveProviderKeys().size());

        // include provider1
        if (provider1 != null) {
            System.out.println("Including provider: " + provider1);
            loadBalancer.includeProvider(provider1);
        }
        System.out.println("Active provider count after include: " + loadBalancer.getActiveProviderKeys().size());
    }

    private static void heartbeat() throws IOException, InterruptedException {
        System.out.println("***************************************************************************");
        System.out.println("******** Starting LoadBalancer run. Press ENTER key to STOP run... ********");
        System.out.println("***************************************************************************");

        loadBalancer.start(false);
        while (System.in.available() == 0) {
            // do nothing. wait until key press
        }
        System.in.read();
        loadBalancer.stop();
    }

    private static void optimizedHeartbeat() throws IOException, InterruptedException {
        System.out.println("************************************************************************************************");
        System.out.println("******** Starting LoadBalancer with optimized heartbeat. Press ENTER key to STOP run... ********");
        System.out.println("************************************************************************************************");

        loadBalancer.start(true);
        while (System.in.available() == 0) {
            // do nothing. wait until key press
        }
        System.in.read();
        loadBalancer.stop();
    }

    private static void invokeParallelRequests() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(loadBalancer);
        loadBalancer.awaitCompletion();
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}
