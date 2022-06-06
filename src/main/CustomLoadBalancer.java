package main;

import main.loadbalancer.LoadBalancer;

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
        randomInvocation(100);

        // Step 4 – Round Robin invocation
        System.out.println("-------------------------------------");
        System.out.println("Step 4 – Round Robin invocation");
        System.out.println("-------------------------------------");
        roundRobinInvocation(100);

        // Step 5 – Manual node exclusion / inclusion
        System.out.println("-------------------------------------");
        System.out.println("Step 5 – Manual node exclusion / inclusion");
        System.out.println("-------------------------------------");
        try {
            includeAndExcludeProvider();
        } catch (LoadBalancerException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void registerProviders(int num) throws LoadBalancerException {
        System.out.println("Registering " + num + " providers...");
        loadBalancer.registerProviders(num);
    }

    private static void randomInvocation(int times) {
        for (int i = 0; i < times; i++) {
            System.out.println("Random invoke from loadbalancer : " + loadBalancer.get(InvocationPattern.RANDOM));
        }
    }

    private static void roundRobinInvocation(int times) {
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
}
