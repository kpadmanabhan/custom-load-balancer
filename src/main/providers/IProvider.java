package main.providers;

public interface IProvider extends Runnable {
    String getId();

    String get();

    boolean check();
}
