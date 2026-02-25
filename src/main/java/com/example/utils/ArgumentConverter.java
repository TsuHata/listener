package com.example.utils;

import lombok.NonNull;

import java.lang.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * A utility class for converting an array of string arguments into an array of objects of specified types. It uses a map of conversion methods provided by the {@link ConverterMethods}
 *  class to perform the conversions.
 */
public class ArgumentConverter {

    private ConverterMethods methodMap;

    /**
     * Returns a new instance of ArgumentConverter initialized with the provided ConverterMethods.
     *
     * @param methods The ConverterMethods object containing the conversion methods. Must not be null.
     * @return A new non-null instance of ArgumentConverter.
     */
    public static @NonNull ArgumentConverter getInstance(@NonNull ConverterMethods methods) {
        ArgumentConverter converter = new ArgumentConverter();
        converter.setMethods(methods);
        return converter;
    }

    /**
     * Returns a new instance of ArgumentConverter initialized with the default ConverterMethods.
     * @return  A new non-null instance of ArgumentConverter.
     */
    public static @NonNull ArgumentConverter getInstance() {
        ArgumentConverter converter = new ArgumentConverter();
        converter.setMethods(new ConverterMethods());
        return converter;
    }

    /**
     * Converts an array of string arguments to an array of objects based on the specified types.
     *
     * @param args An array of strings representing the values to be converted.
     * @param types An array of Class objects representing the target types for each argument in `args`.
     * @return An array of Objects where each element is the result of converting the corresponding element in `args` to the type specified in `types`.
     * @throws NoSuchFieldException if the method map is not initialized, indicating that the ArgumentConverter instance was not created using the static `getInstance()` method.
     * @throws IllegalArgumentException if the length of `args` does not match the length of `types`.
     * @throws InvocationTargetException if an error occurs during the invocation of a conversion method.
     * @throws IllegalAccessException if a security manager denies access to the conversion method.
     */
    public Object[] convert(String[] args, Class<?>[] types) throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        if (methodMap == null) {
            throw new NoSuchFieldException("Cannot get methods, please use static method getInstance() to create an ArgumentConverter Object");
        }
        if (args.length != types.length) {
            throw new IllegalArgumentException("args and types must have the same length");
        }

        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Class<?> type = types[i];
            if (type.equals(String.class)) {
                converted[i] = args[i];
                continue;
            }
            List<Method> methodList = methodMap.getMethods(types[i]);
            for (Method method : methodList) {
                Object result = method.invoke(null, args[i]);
                if (result != null && result.getClass().isAssignableFrom(type)) {
                    converted[i] = result;
                    break;
                }
            }
        }
        return converted;
    }

    private void setMethods(ConverterMethods methods) {
        this.methodMap = methods;
    }

}
