package com.example.core;

import com.example.annotation.Listener;
import com.example.annotation.Parameter;
import com.example.exception.ListenerException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Singleton enum for managing a collection of listener managers.
 * This container provides methods to register objects with specific or default listener managers.
 * It ensures that each named manager is created only once and is thread-safe.
 */
public enum ListenerContainer {
    INSTANCE;

    private final Map<String, ListenerManager> managers = new ConcurrentHashMap<>();

    /**
     * Retrieves the default {@link ListenerManager} instance.
     * This method is a convenient way to access the manager associated with the "default" key.
     *
     * @return the default {@link ListenerManager} used for registering objects without specifying a manager name
     */
    public ListenerManager getDefaultManager() {
        return getManager("default");
    }

    /**
     * Retrieves a {@link ListenerManager} instance associated with the given name.
     * If no manager is found for the specified name, a new one is created and stored.
     *
     * @param name the name of the listener manager to retrieve
     * @return the {@link ListenerManager} instance corresponding to the provided name
     */
    public ListenerManager getManager(String name) {
        return managers.computeIfAbsent(name, k -> new ListenerManager());
    }

    /**
     * Registers the provided object with the default listener manager for processing.
     * This method facilitates the registration of objects that need to be scanned for
     * listener and parameter annotations. The object is registered with the default
     * {@link ListenerManager} which is responsible for handling all unspecified manager cases.
     *
     * @param obj the object to register. It will be scanned for fields and methods annotated
     *            with {@link Listener} or {@link Parameter}. Must not be null.
     * @throws ListenerException if an error occurs while registering the object.
     */
    public void register(Object obj) {
        getDefaultManager().registryObject(obj);
    }

    /**
     * Registers the provided object with a specific listener manager for processing.
     * This method facilitates the registration of objects that need to be scanned for
     * listener and parameter annotations. The object is registered with the listener
     * manager specified by the given name, which is responsible for handling the
     * annotated fields and methods within the object.
     *
     * @param managerName the name of the listener manager to use for registering the object
     * @param obj the object to register. It will be scanned for fields and methods annotated
     *            with {@link Listener} or {@link Parameter}. Must not be null.
     * @throws ListenerException if an error occurs while registering the object.
     */
    public void register(String managerName, Object obj) {
        getManager(managerName).registryObject(obj);
    }
}
