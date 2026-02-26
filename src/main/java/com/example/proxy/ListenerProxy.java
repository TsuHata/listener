package com.example.proxy;

import com.example.annotation.Listener;
import com.example.core.ListenerManager;
import com.example.exception.ListenerException;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
        field.setAccessible(true);
        Object original = field.get(target);

        if (original == null) {
            throw new ListenerException("Cannot create proxy for a null field: "
                    + field.getName() + " in " + target.getClass().getName() + ": " + listenerName);
        }

        Object proxy = Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class<?>[]{field.getType()},
                (proxyObj, method, args) -> {
                    Object result = method.invoke(original, args);

                    if (isSetterMethod(method) && args != null && args.length > 0) {
                        manager.notifyListenerUpdate(listenerName);
                    }

                    return result;
                }
        );

        field.set(target, proxy);
        return proxy;
    }

    private boolean isSetterMethod(Method method) {
        String name = method.getName();
        return name.startsWith("set") &&
                name.length() > 3 &&
                method.getParameterCount() == 1 &&
                method.getReturnType() == void.class;
    }
}
