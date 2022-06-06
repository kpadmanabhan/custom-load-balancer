package main.providers;

import java.util.Random;
import java.util.UUID;

public class DefaultProvider implements IProvider {

    // UUID is not guaranteed to be unique
    // but for the scope of this example, practically we will get unique id without conflict
    private String providerId = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return providerId;
    }

    @Override
    public String get() {
        return getId();
    }

    @Override
    public boolean check() {
        return new Random().nextBoolean();
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
        System.out.println("Provider: " + get());
    }
}
