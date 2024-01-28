/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui;


import com.vaadin.flow.component.Component;

import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.common.FormValidationException;

public interface ColumnComponent 
{
	void refresh();
	void validate() throws FormValidationException;
	void setConfigState(AuthnElementConfiguration state);
	void addValueChangeListener(Runnable valueChange);
	AuthnElementConfiguration getConfigState();
	Component getComponent();
}
