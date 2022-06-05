package main.providers;

import java.util.UUID;

public class DefaultProvider implements IProvider {

    // UUID is not guaranteed to be unique
    // but for the scope of this example, practically we will get unique id without conflict
    private String providerId = UUID.randomUUID().toString();

    @Override
    public String get() {
        return providerId;
    }
}
