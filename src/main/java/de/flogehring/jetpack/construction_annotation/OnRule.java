package de.flogehring.jetpack.construction_annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(MapAbstract.class)
public @interface OnRule {

    String rule();
    Class<?> clazz();
}
