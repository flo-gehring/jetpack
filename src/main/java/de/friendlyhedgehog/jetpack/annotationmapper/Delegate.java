package de.friendlyhedgehog.jetpack.annotationmapper;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(MapAbstract.class)
public @interface Delegate {

    Class<?> clazz();
    boolean transitive() default false;
}
