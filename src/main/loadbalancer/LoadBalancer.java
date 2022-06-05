package main.loadbalancer;

import main.InvocationPattern;
import main.providers.DefaultProvider;
import main.providers.IProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
    private final Map<String, IProvider> providers = new HashMap<>();
    private int lastInvokedProviderIndex = -1;

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
    public void register(int num) {
        for (int i = 0; i < num; i++) {
            if (providers.size() >= MAX_PROVIDERS) {
                // cannot register anymore. break registration
                break;
            }

            IProvider provider = new DefaultProvider();
            providers.put(provider.get(), provider);
        }
    }

    private IProvider getRandomProvider() {
        var randomKey = providers.keySet().toArray()[new Random().nextInt(providers.size())];
        return providers.get(randomKey);
    }

    private IProvider getNextProvider() {
        // if last provider is invoked, start from 0 again, else invoke next provider
        var providerIndex =
                lastInvokedProviderIndex == providers.size() - 1 ?
                        0:
                        ++lastInvokedProviderIndex;
        var nextProviderKey = providers.keySet().toArray()[providerIndex];
        return providers.get(nextProviderKey);
    }
}
