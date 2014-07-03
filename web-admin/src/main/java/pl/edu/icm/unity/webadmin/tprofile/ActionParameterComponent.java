/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.ui.Component;

/**
 * Implementations are used to edit values of translation action parameters.
 * @author K. Benedyczak
 */
public interface ActionParameterComponent extends Component
{
	String getActionValue();
	void setActionValue(String value);
	void setValidationVisible(boolean how);
	boolean isValid();
}
