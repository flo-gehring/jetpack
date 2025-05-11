package de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
/// Companion annotation to [de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreationStrategyConstructor].
public @interface CreatorConstructor {
    String[] order();

}
