/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sessionscope;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines a Spring Component of web session scope. Such component will have a single instance per (vaadin) web session.
 */
@Component
@Scope(WebSessionScope.NAME)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSessionComponent {

}
