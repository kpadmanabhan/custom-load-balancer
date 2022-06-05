package main;

import main.loadbalancer.LoadBalancer;

import java.util.ArrayList;

/**
 * This class acts as the main entry point as well as the client hitting the load balancer
 */
public class CustomLoadBalancer {
    private static LoadBalancer loadBalancer;

    public static void main(String[] args) {
        loadBalancer = LoadBalancer.getInstance();

        // Step 2 – Register a list of providers
        loadBalancer.register(10);

        // Step 3 – Random invocation
        randomInvocation(100);

        // Step 4 – Round Robin invocation
        roundRobinInvocation(100);
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
}
