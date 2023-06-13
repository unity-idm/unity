/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.attributeTypes;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * Allows to edit an attribute type which has immutable type. For such attributes only displayed name
 * and description can be edited. Creation of an attribute type is not possible. 
 * 
 * @author K. Benedyczak
 */
class ImmutableAttributeTypeEditor extends FormLayout implements AttributeTypeEditor
{
	private MessageSource msg;
	
	private AttributeType original;
	private Label name;
	private I18nTextField displayedName;
	private I18nTextArea typeDescription;
	private CheckBox selfModificable;
	
	ImmutableAttributeTypeEditor(MessageSource msg, AttributeType toEdit)
	{
		super();
		this.msg = msg;
		original = toEdit;
		
		initUI(toEdit);
	}

	private void initUI(AttributeType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new Label(toEdit.getName());
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("AttributeType.displayedName"));
		addComponent(displayedName);
		
		typeDescription = new I18nTextArea(msg, msg.getMessage("AttributeType.description"));
		addComponent(typeDescription);
		
		selfModificable = new CheckBox(msg.getMessage("AttributeType.selfModificableCheck"));
		addComponent(selfModificable);
		
		setInitialValues(toEdit);
	}
	
	private void setInitialValues(AttributeType aType)
	{
		typeDescription.setValue(aType.getDescription());
		displayedName.setValue(aType.getDisplayedName());
		selfModificable.setValue(aType.isSelfModificable());
	}
	
	@Override
	public AttributeType getAttributeType() throws IllegalAttributeTypeException
	{
		AttributeType ret = new AttributeType();
		ret.setDescription(typeDescription.getValue());
		ret.setName(name.getValue());
		I18nString displayedNameS = displayedName.getValue();
		displayedNameS.setDefaultValue(ret.getName());
		ret.setDisplayedName(displayedNameS);
		ret.setValueSyntax(original.getValueSyntax());
		ret.setValueSyntaxConfiguration(original.getValueSyntaxConfiguration());
		ret.setSelfModificable(selfModificable.getValue());
		return ret;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
