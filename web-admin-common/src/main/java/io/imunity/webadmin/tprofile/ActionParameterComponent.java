/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import com.vaadin.ui.Component;

/**
 * Implementations are used to edit values of translation action parameters.
 * @author K. Benedyczak
 */
public interface ActionParameterComponent extends Component
{
	String getActionValue();
	void setActionValue(String value);
	boolean isValid();
	void setReadOnly(boolean readOnly);
	void addValueChangeCallback(Runnable callback);
}
