/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.List;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.attribute.AttributeEditDialog;
import pl.edu.icm.unity.webadmin.attribute.AttributeEditor;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementWebHandlerFactory.AttributeStatementComponent;
import pl.edu.icm.unity.webui.common.FlexibleFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
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
	protected List<AttributeType> attributeTypes; 
	protected String group;
	
	protected FlexibleFormLayout main;
	private TextField assignedAttributeTF;
	private TextField conditionAttributeTF;
	private Attribute<?> assignedAttribute;
	private Attribute<?> conditionAttribute;
	
	public AbstractAttributeStatementComponent(UnityMessageSource msg,
			AttributeHandlerRegistry attrHandlerRegistry,
			List<AttributeType> attributeTypes, String group, String description)
	{
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.attributeTypes = attributeTypes;
		this.group = group;
		this.main = getMainLayout(description);
	}
	
	private FlexibleFormLayout getMainLayout(String description)
	{
		FlexibleFormLayout ret = new FlexibleFormLayout();
		ret.setMargin(true);
		ret.setSizeFull();
		
		Label descriptionL = new Label();
		descriptionL.setCaption(msg.getMessage("AttributeStatements.description"));
		descriptionL.setValue(description);
		ret.addLine(descriptionL);
		return ret;
	}


	@Override
	public Component getComponent()
	{
		return main;
	}
	
	protected void addAssignedAttributeField()
	{
		assignedAttributeTF = new TextField(msg.getMessage("AttributeStatementEditDialog.assignedAttribute"));
		assignedAttributeTF.setValue(msg.getMessage("AttributeStatementEditDialog.noAttribute"));
		assignedAttributeTF.setReadOnly(true);
		assignedAttributeTF.setWidth(100, Unit.PERCENTAGE);
		Button editAssignedAttribute = new Button();
		editAssignedAttribute.setIcon(Images.edit.getResource());
		editAssignedAttribute.setDescription(msg.getMessage("AttributeStatementEditDialog.edit"));
		editAssignedAttribute.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				editAssignedAttribute();
			}
		});
		main.addLine(assignedAttributeTF, editAssignedAttribute);
	}

	protected void addConditionAttributeField()
	{
		conditionAttributeTF = new TextField(msg.getMessage("AttributeStatementEditDialog.conditionAttribute"));
		conditionAttributeTF.setValue(msg.getMessage("AttributeStatementEditDialog.noAttribute"));
		conditionAttributeTF.setReadOnly(true);
		conditionAttributeTF.setWidth(100, Unit.PERCENTAGE);
		Button editAttribute = new Button();
		editAttribute.setIcon(Images.edit.getResource());
		editAttribute.setDescription(msg.getMessage("AttributeStatementEditDialog.edit"));
		editAttribute.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				editConditionAttribute();
			}
		});
		main.addLine(conditionAttributeTF, editAttribute);
	}

	private void checkAttributeSet(Attribute<?> attribute, TextField attributeTF) throws FormValidationException
	{
		if (attribute == null)
		{
			attributeTF.setComponentError(new UserError(
					msg.getMessage("AttributeStatementEditDialog.attributeRequired")));
			throw new FormValidationException();
		}
	}
	
	protected Attribute<?> getAssignedAttribute() throws FormValidationException
	{
		checkAttributeSet(assignedAttribute, assignedAttributeTF);
		for (AttributeType type: attributeTypes)
		{
			if (type.getName().equals(assignedAttribute.getName()))
			{
				try
				{
					AttributeValueChecker.validate(assignedAttribute, type);
				} catch (Exception e)
				{
					assignedAttributeTF.setComponentError(new UserError(msg.getMessage(
							"AttributeStatementEditDialog.attributeInvalid", e.getMessage())));
					throw new FormValidationException();
				}
			}
		}
		assignedAttributeTF.setComponentError(null);
		return assignedAttribute;
	}
	
	protected Attribute<?> getConditionAttribute() throws FormValidationException
	{
		checkAttributeSet(conditionAttribute, conditionAttributeTF);
		conditionAttributeTF.setComponentError(null);
		return conditionAttribute;
	}
	
	
	protected void setAssignedAttribute(Attribute<?> newAttribute)
	{
		if (newAttribute != null)
			setAttributeField(assignedAttributeTF, newAttribute);
	}

	protected void setConditionAttribute(Attribute<?> newAttribute)
	{
		if (newAttribute != null)
			setAttributeField(conditionAttributeTF, newAttribute);
	}
	
	private void editAssignedAttribute()
	{
		editAttribute(assignedAttribute, group, new AttributeEditDialog.Callback()
		{
			@Override
			public boolean newAttribute(Attribute<?> newAttribute)
			{
				setAttributeField(assignedAttributeTF, newAttribute);
				assignedAttribute = newAttribute;
				return true;
			}
		});
	}

	private void editConditionAttribute()
	{
		editAttribute(conditionAttribute, group, new AttributeEditDialog.Callback()
		{
			@Override
			public boolean newAttribute(Attribute<?> newAttribute)
			{
				setAttributeField(conditionAttributeTF, newAttribute);
				conditionAttribute = newAttribute;
				return true;
			}
		});
	}
	
	private void editAttribute(Attribute<?> initial, String groupPath, AttributeEditDialog.Callback callback)
	{
		AttributeEditor theEditor = new AttributeEditor(msg, attributeTypes, groupPath, attrHandlerRegistry);
		if (initial != null)
			theEditor.setInitialAttribute(initial);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, 
				msg.getMessage("AttributeStatementEditDialog.attributeEdit"), callback, theEditor);
		dialog.show();
	}
	
	private void setAttributeField(TextField tf, Attribute<?> assignedAttribute)
	{
		String attrRep = attrHandlerRegistry.getSimplifiedAttributeRepresentation(assignedAttribute,
				AttributeHandlerRegistry.DEFAULT_MAX_LEN);
		tf.setReadOnly(false);
		tf.setValue(attrRep);
		tf.setReadOnly(true);
		tf.setComponentError(null);
	}

}
