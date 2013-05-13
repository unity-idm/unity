/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.types.basic.AttributeStatementCondition;
import pl.edu.icm.unity.webui.common.FormValidationException;

public interface ConditionPanel
{
	public Component getComponent();
	public AttributeStatementCondition getCondition() throws FormValidationException;
	public void setCondition(AttributeStatementCondition condition);
}
