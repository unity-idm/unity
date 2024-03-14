/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.ui;


import com.vaadin.flow.component.Component;
import io.imunity.vaadin.auth.services.layout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

public interface ColumnComponent 
{
	void refresh();
	void validate() throws FormValidationException;
	void setConfigState(AuthnElementConfiguration state);
	void addValueChangeListener(Runnable valueChange);
	AuthnElementConfiguration getConfigState();
	Component getComponent();
}
