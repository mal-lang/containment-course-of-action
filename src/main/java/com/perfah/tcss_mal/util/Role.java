package com.perfah.tcss_mal.util;

import java.lang.annotation.*;
import java.lang.reflect.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Role {
    String name() default "";
    String assetType() default "";
    String description() default "";
}
