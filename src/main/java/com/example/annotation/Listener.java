package com.example.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Listener {
    String listenerName();
    String executorName() default "";
    ListenerMode mode() default ListenerMode.LISTEN;
}
