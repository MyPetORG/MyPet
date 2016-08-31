package de.Keyle.MyPet.api.util.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Load {
    enum State {
        OnEnable, OnLoad, AfterHooks, OnReady
    }

    /**
     * @return the state where the service is loaded
     */
    State value() default State.OnEnable;
}