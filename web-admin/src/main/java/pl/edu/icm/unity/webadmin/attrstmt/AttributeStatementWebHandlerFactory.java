/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.List;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FormValidationException;

public interface AttributeStatementWebHandlerFactory
{
	public String getSupportedAttributeStatementName();
	public AttributeStatementComponent getEditorComponent(List<AttributeType> attributeTypes, String group);
	public String getTextRepresentation(AttributeStatement as);
	
	public interface AttributeStatementComponent
	{
		public Component getComponent();
		public void setInitialData(AttributeStatement initial);
		public AttributeStatement getStatementFromComponent() throws FormValidationException;
	}
}
