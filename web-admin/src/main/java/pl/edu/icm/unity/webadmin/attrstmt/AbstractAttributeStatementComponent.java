/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;

import com.vaadin.server.UserError;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attribute.AttributeFieldWithEdit;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementWebHandlerFactory.AttributeStatementComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Common code for attribute statement handler components.
 * It handles two attribute fields - assigned attribute and condition attribute. It is possible to 
 * actually use one of them or both.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractAttributeStatementComponent implements AttributeStatementComponent
{
	protected UnityMessageSource msg;
	protected AttributeHandlerRegistry attrHandlerRegistry;
	protected Collection<AttributeType> attributeTypes; 
	protected String group;
	
	protected FormLayout main;
	private AttributeFieldWithEdit assignedAttribute;
	private AttributeFieldWithEdit conditionAttribute;
	
	public AbstractAttributeStatementComponent(UnityMessageSource msg,
			AttributeHandlerRegistry attrHandlerRegistry,
			Collection<AttributeType> attributeTypes, String group, String description)
	{
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.attributeTypes = attributeTypes;
		this.group = group;
		this.main = getMainLayout(description);
	}
	
	private FormLayout getMainLayout(String description)
	{
		FormLayout ret = new FormLayout();
		ret.setMargin(true);
		ret.setSizeFull();
		
		Label descriptionL = new Label();
		descriptionL.setCaption(msg.getMessage("AttributeStatements.description"));
		descriptionL.setValue(description);
		ret.addComponent(descriptionL);
		return ret;
	}


	@Override
	public Component getComponent()
	{
		return main;
	}
	
	protected void addAssignedAttributeField()
	{
		assignedAttribute = new AttributeFieldWithEdit(msg, 
				msg.getMessage("AttributeStatementEditDialog.assignedAttribute"), 
				attrHandlerRegistry, attributeTypes, group, null);
		assignedAttribute.setWidth(100, Unit.PERCENTAGE);
		main.addComponent(assignedAttribute);
	}

	protected void addConditionAttributeField()
	{
		conditionAttribute = new AttributeFieldWithEdit(msg, 
				msg.getMessage("AttributeStatementEditDialog.conditionAttribute"),
				attrHandlerRegistry, attributeTypes, group, null);
		conditionAttribute.setWidth(100, Unit.PERCENTAGE);
		main.addComponent(conditionAttribute);
	}


	protected Attribute<?> getAssignedAttribute() throws FormValidationException
	{
		Attribute<?> ret = assignedAttribute.getAttribute();
		for (AttributeType type: attributeTypes)
		{
			if (type.getName().equals(ret.getName()))
			{
				try
				{
					AttributeValueChecker.validate(ret, type);
				} catch (Exception e)
				{
					assignedAttribute.setError(new UserError(msg.getMessage(
							"AttributeStatementEditDialog.attributeInvalid", e.getMessage())));
					throw new FormValidationException();
				}
			}
		}
		return ret;
	}
	
	protected Attribute<?> getConditionAttribute() throws FormValidationException
	{
		return conditionAttribute.getAttribute();
	}
	
	
	protected void setAssignedAttribute(Attribute<?> newAttribute)
	{
		if (newAttribute != null)
			assignedAttribute.setAttribute(newAttribute);
	}

	protected void setConditionAttribute(Attribute<?> newAttribute)
	{
		if (newAttribute != null)
			conditionAttribute.setAttribute(newAttribute);
	}
}
