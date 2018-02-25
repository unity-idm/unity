/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.Collection;

import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Custom component displaying a simple, textual attribute and its values, additionally 
 * allowing to edit the attribute after clicking an edit button.
 * 
 * @author K. Benedyczak
 */
public class AttributeFieldWithEdit extends CustomField<Attribute>
{
	private UnityMessageSource msg;
	protected AttributeHandlerRegistry attrHandlerRegistry;
	protected Collection<AttributeType> attributeTypes;
	protected String group;
	private TextField attributeTF;
	private HorizontalLayout hl;
	private boolean valuesRequired;
	
	private Attribute attribute;
	private AttributeType fixedAttributeType;
	
	public AttributeFieldWithEdit(UnityMessageSource msg, String caption, 
			AttributeHandlerRegistry attrHandlerRegistry,
			Collection<AttributeType> attributeTypes, String group, Attribute initial,
			boolean valuesRequired)
	{
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.group = group;
		this.valuesRequired = valuesRequired;
		this.attributeTypes = attributeTypes;
		setCaption(caption);
		this.attribute = initial;
		attributeTF = new TextField();
		attributeTF.setValue(msg.getMessage("AttributeField.noAttribute"));
		attributeTF.setReadOnly(true);
		attributeTF.addValueChangeListener(e -> fireEvent(e));
		Button editAssignedAttribute = new Button();
		editAssignedAttribute.setIcon(Images.edit.getResource());
		editAssignedAttribute.setDescription(msg.getMessage("AttributeField.edit"));
		editAssignedAttribute.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				editAttribute();
			}
		});
		hl = new HorizontalLayout();
		hl.setSpacing(false);
		hl.setMargin(false);
		hl.addComponents(attributeTF, editAssignedAttribute);
	}
	
	
	@Override
	protected Component initContent()
	{
		return hl;
	}

	private void editAttribute()
	{
		AttributeEditor theEditor = fixedAttributeType == null ? 
				new AttributeEditor(msg, attributeTypes, null, group, attrHandlerRegistry, valuesRequired) :
				new AttributeEditor(msg, fixedAttributeType, null, group, attrHandlerRegistry);
		if (attribute != null)
			theEditor.setInitialAttribute(attribute);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, 
				msg.getMessage("AttributeField.edit"), new AttributeEditDialog.Callback()
				{
					@Override
					public boolean newAttribute(Attribute newAttribute)
					{
						setAttribute(newAttribute);
						return true;
					}
				}, theEditor);
		dialog.show();
	}
	
	private void checkAttributeSet() throws FormValidationException
	{
		if (attribute == null)
		{
			attributeTF.setComponentError(new UserError(
					msg.getMessage("AttributeField.attributeRequired")));
			throw new FormValidationException();
		}
		attributeTF.setComponentError(null);
	}
	
	public Attribute getAttribute() throws FormValidationException
	{
		checkAttributeSet();
		return attribute;
	}
	
	/**
	 * Sets an attribute to be edited
	 * @param attribute
	 */
	public void setAttribute(Attribute attribute)
	{
		this.attribute = attribute;
		String attrRep = attrHandlerRegistry.getSimplifiedAttributeRepresentation(attribute);
		attributeTF.setReadOnly(false);
		attributeTF.setValue(attrRep);
		attributeTF.setReadOnly(true);
		attributeTF.setComponentError(null);
	}
	
	/**
	 * After call the class will allow to edit only the selected attribute type
	 * @param attributeType
	 */
	public void setFixedType(AttributeType attributeType)
	{
		this.fixedAttributeType = attributeType;
	}
	
	public void setError(ErrorMessage message)
	{
		attributeTF.setComponentError(message);
	}

	@Override
	public Attribute getValue() 
	{
		try
		{
			return getAttribute();
		} catch (FormValidationException e)
		{
			return null;
		}
	}


	@Override
	protected void doSetValue(Attribute value)
	{
		setAttribute(value);
		
	}
}
