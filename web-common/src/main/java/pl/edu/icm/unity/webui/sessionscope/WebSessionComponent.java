/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sessionscope;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Defines a Spring Component of web session scope. Such component will have a single instance per (vaadin) web session.
 */
@Component
@Scope(WebSessionScope.NAME)
public @interface WebSessionComponent {

}
