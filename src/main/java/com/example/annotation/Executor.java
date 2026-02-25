package com.example.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Executor {
    String executorName();
    String[] arguments() default "";
    ExecutorMode mode() default ExecutorMode.NO_PARAMETER;
}
