package de.flogehring.jetpack.annotationmapper.creationstrategies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
/// The class needs a Constructor annotated with [de.flogehring.jetpack.annotationmapper.creationstrategies.CreatorConstructor].
public @interface CreationStrategyConstructor {
}
