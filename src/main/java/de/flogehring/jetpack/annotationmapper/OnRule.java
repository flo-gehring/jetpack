package de.flogehring.jetpack.annotationmapper;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(MapAbstract.class)
public @interface OnRule {

    String rule();
    Class<?> clazz();
}
