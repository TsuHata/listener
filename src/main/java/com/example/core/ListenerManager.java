package com.example.core;

import com.example.annotation.Executor;
import com.example.annotation.Listener;
import com.example.annotation.ListenerMode;
import com.example.annotation.Parameter;
import com.example.exception.ListenerException;
import com.example.proxy.ListenerProxy;
import com.example.utils.ArgumentConverter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the registration, unregistration, and invocation of listeners and executors.
 * This class provides methods to register objects for listener and parameter processing,
 * scan fields and methods for specific annotations, and manage the lifecycle of listeners
 * and executors.
 */
@NoArgsConstructor
public class ListenerManager {
    private final Map<String, List<ListenerInfo>> listenerMap = new ConcurrentHashMap<>();
    private final Map<String, ExecutorInfo> executorMap = new ConcurrentHashMap<>();
    private final Map<String, List<ParameterInfo>> parameterMap = new ConcurrentHashMap<>();
    private final ListenerProxy listenerProxy = new ListenerProxy(this);

    /**
     * Registers the provided object for listener and parameter processing. This method scans
     * the fields and methods of the given object, identifying those annotated with {@link Listener}
     * or {@link Parameter}. It then processes these annotations, registering listeners and parameters
     * as needed. If an error occurs during the registration process, a {@link ListenerException} is thrown.
     *
     * @param obj The object to be registered. Must not be null.
     * @throws ListenerException if an error occurs while registering the object.
     */
    public final void registryObject(@NonNull Object obj) {
        try {
            scanField(obj);
            scanMethod(obj);
        } catch (Exception e) {
            throw new ListenerException("error occurs while registry an object: " + obj.getClass().getName(), e);
        }
    }

    /**
     * Registers multiple objects for listener and parameter processing. This method iterates over the
     * provided array of objects, invoking {@link #registryObject(Object)} on each one to scan and
     * process their fields and methods for annotations like {@link Listener} or {@link Parameter}.
     * If an error occurs during the registration of any object, a {@link ListenerException} is thrown.
     *
     * @param objs The array of objects to be registered. Each object must not be null.
     * @throws ListenerException if an error occurs while registering any of the objects.
     */
    public final void registryObjects(Object... objs) {
        for (Object obj : objs) {
            registryObject(obj);
        }
    }

    /**
     * Scans the fields of the provided object for annotations and processes them.
     * If a field is annotated with {@link Listener}, it creates a {@link ListenerInfo} object
     * and adds it to the listener map. It also creates a proxy for the field.
     * If a field is annotated with {@link Parameter}, it creates a {@link ParameterInfo} object
     * and adds it to the parameter map.
     *
     * @param obj The object whose fields are to be scanned. Must not be null.
     * @throws Exception if an error occurs during the scanning or processing of the fields.
     */
    private void scanField(@NonNull Object obj) throws Exception {
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            Listener listener = field.getAnnotation(Listener.class);
            if (listener != null) {
                ListenerInfo info = new ListenerInfo(obj, field, listener);
                listenerMap.computeIfAbsent(
                        listener.listenerName(),
                        k -> new CopyOnWriteArrayList<>()
                ).add(info);
                listenerProxy.createFieldProxy(obj, field, listener.listenerName());
            }

            Parameter parameter = field.getAnnotation(Parameter.class);
            if (parameter != null) {
                ParameterInfo info = new ParameterInfo(obj, field, parameter);
                parameterMap.computeIfAbsent(
                        parameter.executorName(),
                        k -> new CopyOnWriteArrayList<>()
                ).add(info);
            }
        }
    }

    /**
     * Scans the methods of the provided object for the {@link Executor} annotation and processes them.
     * If a method is annotated with {@link Executor}, it creates an {@link ExecutorInfo} object
     * and adds it to the executor map using the executor name as the key.
     *
     * @param obj The object whose methods are to be scanned. Must not be null.
     */
    private void scanMethod(@NonNull Object obj) {
        Class<?> clazz = obj.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            Executor executor = method.getAnnotation(Executor.class);
            if (executor != null) {
                ExecutorInfo info = new ExecutorInfo(obj, method, executor);
                executorMap.put(executor.executorName(), info);
            }
        }
    }

    /**
     * Notifies and updates the listeners with the specified name.
     * <p>
     * This method looks up all {@link ListenerInfo} objects associated with the given listener name
     * from the internal map. It then iterates over these listeners, checking if their mode is set to
     * {@link ListenerMode#UPDATE}. For each listener in update mode, it attempts to invoke the
     * corresponding executor by its name. The result of the executor's invocation is then used to
     * update the field of the listener's associated bean.
     *
     * @param listenerName the name of the listener to be notified and updated
     * @throws ListenerException if no listener is found for the given name, or if there are issues
     *                           with invoking the executor or updating the listener's field
     */
    public void notifyListenerUpdate(String listenerName) {
        List<ListenerInfo> infos = listenerMap.get(listenerName);
        if (infos == null || infos.isEmpty()) {
            throw new ListenerException("no listener found: " + listenerName);
        }

        for (ListenerInfo info : infos) {
            if (info.listener.mode().equals(ListenerMode.UPDATE)) {
                String executorName = info.listener.executorName();
                if (executorName.isEmpty()) {
                    throw new ListenerException("no executor found from: " + listenerName);
                }

                try {
                    Object result = invokeExecutor(executorName);
                    info.field().set(info.bean(), result);
                } catch (IllegalAccessException e) {
                    throw new ListenerException("Failed to update listener field: " + listenerName, e);
                }
            }
        }
    }

    /**
     * Invokes the specified executor by its name.
     *
     * @param executorName the name of the executor to be invoked
     * @return the result of the executor method invocation
     * @throws ListenerException if the executor is not found or an error occurs during the invocation
     */
    private Object invokeExecutor(String executorName) {
        ExecutorInfo info = executorMap.get(executorName);
        if (info == null) {
            throw new ListenerException("Executor not found: " + executorName);
        }

        try {
            Object[] args = resolveParameters(info);
            return info.method.invoke(info.bean, args);
        } catch (Exception e) {
            throw new ListenerException("Failed to invoke executor: " + executorName, e);
        }
    }

    /**
     * Resolves the parameters for a given executor based on its mode and parameter types.
     *
     * @param executorInfo The ExecutorInfo containing the executor, method, and associated bean.
     * @return An array of objects representing the resolved parameters for the executor's method.
     * @throws ListenerException if there is a mismatch in the number or type of parameters, or if an error occurs during parameter resolution.
     */
    private Object[] resolveParameters(@NonNull ExecutorInfo executorInfo) throws Exception {
        Executor executor = executorInfo.executor;
        Method method = executorInfo.method;
        Class<?>[] parameterTypes = method.getParameterTypes();

        switch (executor.mode()) {
            case NO_PARAMETER -> {
                if (parameterTypes.length != 0) {
                    throw new ListenerException("Method has parameters but mode is NO_PARAMETER: " + executor.executorName());
                }
                return new Object[0];
            }

            case ARGUMENTS -> {
                return ArgumentConverter.getInstance().convert(executor.arguments(), parameterTypes);
            }

            case PARAMETERS -> {
                List<ParameterInfo> parameterInfos = parameterMap.get(executor.executorName());
                if (parameterInfos == null || parameterInfos.isEmpty() || parameterInfos.size() != parameterTypes.length) {
                    throw new ListenerException("Parameter count mismatch for executor: " + executor.executorName());
                }

                List<ParameterInfo> sortedParams = parameterInfos.stream()
                        .sorted(Comparator.comparingInt(p -> p.parameter.location()))
                        .toList();

                Object[] paramValues = new Object[sortedParams.size()];
                for (int i = 0; i < sortedParams.size(); i++) {
                    ParameterInfo param = sortedParams.get(i);
                    try {
                        Object value = param.field.get(param.bean);
                        if (value == null || !parameterTypes[i].isAssignableFrom(value.getClass())) {
                            throw new ListenerException("parameter type mismatch for executor: " + executor.executorName());
                        }
                        paramValues[i] = value;
                    } catch (IllegalAccessException e) {
                        throw new ListenerException("error getting parameter: " + executor.executorName(), e);
                    }
                }
                return paramValues;
            }

            default -> {
                return new Object[0];
            }
        }
    }

    /**
     * Retrieves the value of a field associated with a listener, provided that the listener is in LISTEN mode.
     *
     * @param listenerName The name of the listener whose field value is to be retrieved.
     * @return The value of the field if the listener is found and is in LISTEN mode, otherwise null.
     * @throws ListenerException If an error occurs while trying to retrieve the listener value.
     */
    public Object getListenerValue(String listenerName) {
        List<ListenerInfo> infos = listenerMap.get(listenerName);
        if (infos == null || infos.isEmpty()) {
            return null;
        }

        try {
            for (ListenerInfo info : infos) {
                if (info.listener.mode().equals(ListenerMode.LISTEN)) {
                    return info.field.get(info.bean);
                }
            }
        } catch (Exception e) {
            throw new ListenerException("Failed to get listener value: " + listenerName, e);
        }
        return null;
    }

    /**
     * Checks if a listener with the specified name is registered and has at least one associated {@link ListenerInfo}.
     *
     * @param listenerName the name of the listener to check
     * @return true if the listener is registered and has at least one associated {@link ListenerInfo}, false otherwise
     */
    public boolean hasListener(String listenerName) {
        return listenerMap.containsKey(listenerName) && !listenerMap.get(listenerName).isEmpty();
    }

    /**
     * Checks if an executor with the specified name is registered.
     *
     * @param executorName the name of the executor to check
     * @return true if the executor is registered, false otherwise
     */
    public boolean hasExecutor(String executorName) {
        return executorMap.containsKey(executorName);
    }

    /**
     * Returns an unmodifiable list containing the names of all registered listeners.
     *
     * @return an unmodifiable list of strings, each representing the name of a registered listener
     */
    public List<String> getAllListenerNames() {
        return List.copyOf(listenerMap.keySet());
    }

    /**
     * Returns an unmodifiable list containing the names of all registered executors.
     *
     * @return an unmodifiable list of strings, each representing the name of a registered executor
     */
    public List<String> getAllExecutorNames() {
        return List.copyOf(executorMap.keySet());
    }

    /**
     * Unregisters the provided object from the listener, executor, and parameter maps.
     * This method removes all references to the given object in these maps, effectively
     * unregistering it from the ListenerManager. It will remove the object from the listenerMap,
     * executorMap, and parameterMap if it is found within them.
     *
     * @param obj The object to be unregistered. Must not be null.
     */
    public void unregisterObject(@NonNull Object obj) {
        listenerMap.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(info -> info.bean() == obj);
            return entry.getValue().isEmpty();
        });

        executorMap.entrySet().removeIf(entry -> entry.getValue().bean() == obj);

        parameterMap.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(info -> info.bean() == obj);
            return entry.getValue().isEmpty();
        });
    }

    /**
     * Clears all the registered listeners, executors, and parameters from their respective maps.
     * After calling this method, the {@link ListenerManager} will no longer have any registered
     * listeners, executors, or parameters, effectively resetting its state.
     */
    public void clear() {
        listenerMap.clear();
        executorMap.clear();
        parameterMap.clear();
    }

    //内部类定义
    private record ListenerInfo (Object bean, Field field, Listener listener) { }
    private record ExecutorInfo (Object bean, Method method, Executor executor) { }
    private record ParameterInfo(Object bean, Field field, Parameter parameter) { }
}

