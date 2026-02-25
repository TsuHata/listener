package com.example.proxy;

import com.example.annotation.Listener;
import com.example.core.ListenerManager;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;

/**
 * A concrete implementation of the {@link BaseListenerProxy} interface, designed to work
 * in conjunction with a {@link ListenerManager}. This class is responsible for creating
 * proxies for fields annotated with the {@link Listener} annotation. The proxy creation
 * process is facilitated by the methods provided by the associated {@link ListenerManager}.
 *
 * @see BaseListenerProxy
 * @see ListenerManager
 */
@AllArgsConstructor
public class ListenerProxy implements BaseListenerProxy{

    private final ListenerManager manager;

    @Override
    public Object createFieldProxy(Object target, Field field, String listenerName) throws Exception {
        return null;
    }
}
