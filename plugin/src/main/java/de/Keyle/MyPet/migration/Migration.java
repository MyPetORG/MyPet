package de.Keyle.MyPet.migration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Migration {
    String version() default "";

    String minecraftVersion() default "";

    String description();

    String[] dependsOn() default {};
}
