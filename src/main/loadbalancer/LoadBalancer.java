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
public class LoadBalancer implements Runnable {
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
    private static final int MAX_CALLS_PER_PROVIDER = 5;
    private static final int HEALTH_CHECK_SCHEDULE_IN_SEC = 5;

    /**
     * All registered providers that are active
     */
    private final Map<String, IProvider> providers = new HashMap<>();

    /**
     * Excluded providers
     */
    private final Map<String, IProvider> excludedProviders = new HashMap<>();

    /**
     * Keeps track of health check status for unhealthy providers
     */
    private final Map<String, Integer> healthCheckStatusForExcludedProviders = new HashMap<>();

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;

    private int lastInvokedProviderIndex = -1;
    private int activeRunningCalls = 0;

    /**
     * Invoke this method to start LoadBalancer
     */
    public void start(boolean isOptimizedHealthCheck) {
        Runnable healthCheck = () -> {
            excludeUnhealthyProviders();
            if (isOptimizedHealthCheck) {
                includeHealthyProviders();
            }
            System.out.println("*** Active provider count: " + providers.size());
        };

        executorService = Executors.newScheduledThreadPool(1);
        scheduledFuture = executorService.scheduleAtFixedRate(healthCheck, 0, HEALTH_CHECK_SCHEDULE_IN_SEC, TimeUnit.SECONDS);
    }

    private void includeHealthyProviders() {
        var iterator = excludedProviders.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().check()) {
                // health check success. healthy provider
                System.out.println("optimized check: Heartbeat success for excluded provider: " + entry.getKey());
                var count = healthCheckStatusForExcludedProviders.get(entry.getKey());
                healthCheckStatusForExcludedProviders.put(entry.getKey(), ++count);
                if (healthCheckStatusForExcludedProviders.get(entry.getKey()) >= 2) {
                    // 2 consecutive heartbeat success. include this provider to active providers
                    System.out.println("2 consecutive heartbeat success. Including provider to active providers: " + entry.getKey());
                    iterator.remove();
                    healthCheckStatusForExcludedProviders.remove(entry.getKey());
                    providers.putIfAbsent(entry.getKey(), entry.getValue());
                }
            } else {
                // health check fail. unhealthy provider
                System.out.println("optimized check: Heartbeat failure for excluded provider: " + entry.getKey());
                var count = healthCheckStatusForExcludedProviders.get(entry.getKey());
                count--;
                if (count < 0) {
                    count = 0;
                }
                healthCheckStatusForExcludedProviders.put(entry.getKey(), count);
            }
        }
    }

    private void excludeUnhealthyProviders() {
        var iterator = providers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!entry.getValue().check()) {
                System.out.println("basic check: Unhealthy provider. Excluding from load balancer: " + entry.getKey());
                // remove from active providers and add it to excluded providers
                iterator.remove();
                excludedProviders.putIfAbsent(entry.getValue().getId(), entry.getValue());
                healthCheckStatusForExcludedProviders.putIfAbsent(entry.getValue().getId(), 0);
            } else {
                System.out.println("basic check: Healthy provider: " + entry.getKey());
            }
        }
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
    public synchronized String get(InvocationPattern invocationPattern) throws LoadBalancerException {
        activeRunningCalls++;
        notifyAll();

        if (activeRunningCalls > MAX_CALLS_PER_PROVIDER * providers.size()) {
            activeRunningCalls--;
            throw new LoadBalancerException("Max cluster limit exceeded. Cannot execute call...");
        }

        if (invocationPattern == InvocationPattern.RANDOM) {
            // invoke get() of a random provider
            return getRandomProvider().get();
        }

        if (invocationPattern == InvocationPattern.ROUND_ROBIN) {
            // invoke get() of next sequential provider
            return getNextProvider().get();
        }

        activeRunningCalls--;
        if (activeRunningCalls < 0) {
            activeRunningCalls = 0;
        }
        notifyAll();

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
        healthCheckStatusForExcludedProviders.remove(providerKey);
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
        healthCheckStatusForExcludedProviders.putIfAbsent(provider.getId(), 0);
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

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            try {
                get(InvocationPattern.RANDOM);
            } catch (LoadBalancerException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public synchronized void awaitCompletion() throws InterruptedException {
        while (activeRunningCalls > 0) {
            wait();
        }
    }
}
