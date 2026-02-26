package com.example.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A registry for managing and notifying listeners.
 * This class allows for the registration of objects that act as listeners and provides
 * methods to notify these listeners of updates. It uses a {@link ListenerManager} to handle
 * the actual registration and notification processes.
 *
 * <p>It is designed to be thread-safe, using a {@code CopyOnWriteArrayList} to store
 * registered listener objects, which ensures that the list can be safely iterated over
 * even while being modified.
 *
 * @see ListenerManager
 */
@AllArgsConstructor
public class ListenerRegistry {
    @Getter
    private final ListenerManager manager;
    @Getter
    private final List<Object> registeredObjects = new CopyOnWriteArrayList<>();

    public ListenerRegistry() {
        this.manager = new ListenerManager();
    }

    public ListenerRegistry register(Object obj) {
        manager.registryObject(obj);
        registeredObjects.add(obj);
        return this;
    }

    public ListenerRegistry registerAll(Object... objects) {
        for (Object obj : objects) {
            register(obj);
        }
        return this;
    }

    public ListenerRegistry notify(String listenerName) {
        manager.notifyListenerUpdate(listenerName);
        return this;
    }
}
