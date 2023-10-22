/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.attribute_types;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;

import io.imunity.vaadin.elements.LocaleTextAreaDetails;
import io.imunity.vaadin.elements.LocaleTextFieldDetails;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Allows to edit an attribute type which has immutable type. For such
 * attributes only displayed name and description can be edited. Creation of an
 * attribute type is not possible.
 * 
 * @author K. Benedyczak
 */
class ImmutableAttributeTypeEditor extends FormLayout implements AttributeTypeEditor
{
	private final MessageSource msg;

	private AttributeType original;
	private NativeLabel name;
	private LocaleTextFieldDetails displayedName;
	private LocaleTextAreaDetails typeDescription;
	private Checkbox selfModificable;

	ImmutableAttributeTypeEditor(MessageSource msg, AttributeType toEdit)
	{
		this.msg = msg;
		original = toEdit;
		initUI(toEdit);
	}

	private void initUI(AttributeType toEdit)
	{
		setWidthFull();
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		addClassName("big-vaadin-form-item");
		
		name = new NativeLabel(toEdit.getName());
		addFormItem(name, msg.getMessage("AttributeType.name"));

		displayedName = new LocaleTextFieldDetails(new HashSet<>(msg.getEnabledLocales()
				.values()), msg.getLocale(), Optional.empty(), locale -> "");
		addFormItem(displayedName, msg.getMessage("AttributeType.displayedName"));

		typeDescription = new LocaleTextAreaDetails(new HashSet<>(msg.getEnabledLocales()
				.values()), msg.getLocale(), Optional.empty(), locale -> "");

		addFormItem(typeDescription, msg.getMessage("AttributeType.description"));

		selfModificable = new Checkbox();
		addFormItem(selfModificable, msg.getMessage("AttributeType.selfModificableCheck"));

		setInitialValues(toEdit);
	}

	private void setInitialValues(AttributeType aType)
	{
		typeDescription.setValue(aType.getDescription().getLocalizedMap());
		displayedName.setValue(aType.getDisplayedName().getLocalizedMap());
		selfModificable.setValue(aType.isSelfModificable());
	}

	@Override
	public AttributeType getAttributeType() throws IllegalAttributeTypeException
	{
		AttributeType ret = new AttributeType();
		ret.setDescription(convert(typeDescription.getValue()));
		ret.setName(name.getText());
		I18nString displayedNameS = convert(displayedName.getValue());
		displayedNameS.setDefaultValue(ret.getName());
		ret.setDisplayedName(displayedNameS);
		ret.setValueSyntax(original.getValueSyntax());
		ret.setValueSyntaxConfiguration(original.getValueSyntaxConfiguration());
		ret.setSelfModificable(selfModificable.getValue());
		return ret;
	}

	private I18nString convert(Map<Locale, String> localizedValues)
	{
		I18nString i18nString = new I18nString();
		i18nString.addAllValues(localizedValues.entrySet()
				.stream()
				.collect(Collectors.toMap(x -> x.getKey()
						.toString(), Map.Entry::getValue)));
		return i18nString;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
