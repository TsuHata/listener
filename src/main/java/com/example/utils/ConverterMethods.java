package com.example.utils;

import lombok.NonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A utility class that manages and provides methods for converting strings to various Java primitive wrapper types. This class uses a map to store conversion methods for different classes,
 *  allowing for the registration of custom conversion methods.
 */
public class ConverterMethods {

    private final Map<Class<?>, List<Method>> converterMethodsMap= new ConcurrentHashMap<>();

    public ConverterMethods() {
        Map<Class<?>, List<Method>> map = new ConcurrentHashMap<>();
        Class<?>[] classes = new Class<?>[]{
                Integer.class, Long.class,
                Double.class, Float.class,
                Boolean.class, Byte.class, Short.class
        };
        try {
            for (Class<?> clazz : classes) {
                Method method = clazz.getMethod("valueOf", String.class);
                map.computeIfAbsent(
                        clazz,
                        k -> new CopyOnWriteArrayList<>()
                ).add(method);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers a method to be used as a converter for a specific class.
     * The method must take a single String parameter and return an instance of the specified class.
     *
     * @param clazz The class for which the conversion method is being registered.
     * @param method The method that will be used to convert a String to an instance of the provided class.
     * @throws IllegalArgumentException if the provided method does not have a String parameter or does not return the correct type.
     */
    public void registerConverterMethod(@NonNull Class<?> clazz, @NonNull Method method) {
        if (method.getParameterTypes().length != 1
                || !method.getParameterTypes()[0].equals(String.class)
                || !method.getReturnType().equals(clazz)) {
            throw new IllegalArgumentException("Invalid converter method, a valid method should has a String parameter and a correct return type");
        } else if (clazz.equals(String.class)) {
            return;
        }

        converterMethodsMap.computeIfAbsent(
                clazz,
                k -> new CopyOnWriteArrayList<>()
                ).add(method);
    }

    /**
     * Retrieves a list of methods registered for the specified class that can be used to convert a String to an instance of the class.
     *
     * @param clazz The class for which the conversion methods are being retrieved. Must not be null.
     * @return A List of Method objects representing the conversion methods for the specified class, or an empty list if no methods are registered.
     */
    public List<Method> getMethods(@NonNull Class<?> clazz) {
        return converterMethodsMap.get(clazz);
    }

}
