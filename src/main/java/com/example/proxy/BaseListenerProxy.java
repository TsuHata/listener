package com.example.proxy;

import com.example.annotation.Listener;

import java.lang.reflect.Field;

/**
 * Defines a contract for creating a proxy for a specific field within an object, intended
 * to be used in conjunction with the {@link Listener} annotation. This interface is designed
 * to facilitate the creation of dynamic proxies that can intercept and handle method calls
 * on fields annotated as listeners.
 *
 * @see ListenerProxy
 * @see com.example.core.ListenerManager
 */
public interface BaseListenerProxy {
    Object createFieldProxy(Object target, Field field, String listenerName) throws Exception;
}
