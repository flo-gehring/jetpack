package de.flogehring.jetpack.annotationmapper.creationstrategies;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
/// Relies on the class having a zero-argument Constructor and public mutable fields
public @interface CreationStrategyReflection {

}