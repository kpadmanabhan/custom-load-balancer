package main.loadbalancer;

import main.InvocationPattern;
import main.LoadBalancerException;
import main.providers.DefaultProvider;
import main.providers.IProvider;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Singleton LoadBalancer
 */
public class LoadBalancer {
    // private constructor for singleton implementation
    private LoadBalancer() {
    }

    // singleton loadbalancer instance
    private static LoadBalancer loadBalancer;

    public static LoadBalancer getInstance() {
        if (loadBalancer == null) {
            loadBalancer = new LoadBalancer();
        }
        return loadBalancer;
    }


    private static final int MAX_PROVIDERS = 10;
    private static final int HEALTH_CHECK_SCHEDULE_IN_SEC = 5;

    /**
     * All registered providers that are active
     */
    private final Map<String, IProvider> providers = new HashMap<>();

    /**
     * Excluded providers
     */
    private final Map<String, IProvider> excludedProviders = new HashMap<>();

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;

    private int lastInvokedProviderIndex = -1;

    /**
     * Invoke this method to start LoadBalancer
     */
    public void start() {
        Runnable healthCheck = () -> {
            var iterator = providers.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (!entry.getValue().check()) {
                    System.out.println("Unhealthy provider. Excluding from load balancer: " + entry.getKey());
                    // remove from active providers and add it to excluded providers
                    iterator.remove();
                    excludedProviders.putIfAbsent(entry.getValue().getId(), entry.getValue());
                } else {
                    System.out.println("Healthy provider: " + entry.getKey());
                }
            }
            System.out.println("Active provider count: " + providers.size());
        };

        executorService = Executors.newScheduledThreadPool(1);
        scheduledFuture = executorService.scheduleAtFixedRate(healthCheck, 0, HEALTH_CHECK_SCHEDULE_IN_SEC, TimeUnit.SECONDS);
    }

    public void stop() throws InterruptedException {
        System.out.println("Stop triggered. Waiting for running tasks to complete...");
        if (executorService == null || scheduledFuture == null) {
            return;
        }

        if (!scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
        }

        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }

        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Invoke a method on one of the providers
     *
     * @param invocationPattern RANDOM or ROUND-ROBIN
     * @return result of method invocation from provider
     */
    public String get(InvocationPattern invocationPattern) {
        if (invocationPattern == InvocationPattern.RANDOM) {
            // invoke get() of a random provider
            return getRandomProvider().get();
        }

        if (invocationPattern == InvocationPattern.ROUND_ROBIN) {
            // invoke get() of next sequential provider
            return getNextProvider().get();
        }

        return null;
    }

    /**
     * Register providers
     *
     * @param num number of providers to register
     */
    public void registerProviders(int num) throws LoadBalancerException {
        for (int i = 0; i < num; i++) {
            if (providers.size() >= MAX_PROVIDERS) {
                throw new LoadBalancerException("Loadbalancer is running at max capacity. Consider removing some providers");
            }

            IProvider provider = new DefaultProvider();
            providers.putIfAbsent(provider.getId(), provider);
        }
    }

    /**
     * Returns all active providers
     *
     * @return
     */
    public List<String> getActiveProviderKeys() {
        return List.copyOf(providers.keySet());
    }

    /**
     * Add a specific provider into the loadbalancer
     *
     * @param providerKey Provider key of provider to be included into load balancer
     */
    public void includeProvider(String providerKey) throws LoadBalancerException {
        if (!excludedProviders.containsKey(providerKey)) {
            throw new LoadBalancerException("No excluded provider found with id: " + providerKey);
        }

        // remove from excluded providers and add it to active providers
        var provider = excludedProviders.remove(providerKey);
        providers.putIfAbsent(provider.getId(), provider);
    }

    /**
     * Remove a specific provider into the loadbalancer
     *
     * @param providerKey Provider key of provider to be excluded from load balancer
     */
    public void excludeProvider(String providerKey) throws LoadBalancerException {
        if (!providers.containsKey(providerKey)) {
            throw new LoadBalancerException("Provider " + providerKey + " not registered under the load balancer.");
        }

        // remove from active providers and add it to excluded providers
        var provider = providers.remove(providerKey);
        excludedProviders.putIfAbsent(provider.getId(), provider);
    }

    private IProvider getRandomProvider() {
        var randomKey = providers.keySet().toArray()[new Random().nextInt(providers.size())];
        return providers.get(randomKey);
    }

    private IProvider getNextProvider() {
        // if last provider is invoked, start from 0 again, else invoke next provider
        lastInvokedProviderIndex++;
        if (lastInvokedProviderIndex >= providers.size()) {
            lastInvokedProviderIndex = 0;
        }
        var nextProviderKey = providers.keySet().toArray()[lastInvokedProviderIndex];
        return providers.get(nextProviderKey);
    }
}
