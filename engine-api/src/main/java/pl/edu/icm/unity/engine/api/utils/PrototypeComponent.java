/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines a Spring Component of prototype scope. Shorter version with one simple annotation.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PrototypeComponent {

}
