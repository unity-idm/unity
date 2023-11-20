/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.attribute_types;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.LocalizedTextAreaDetails;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.VaadinClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

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
	private TextField name;
	private LocalizedTextFieldDetails displayedName;
	private LocalizedTextAreaDetails typeDescription;
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
		addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		
		name = new TextField();
		name.setValue(toEdit.getName());
		name.setReadOnly(true);
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		addFormItem(name, msg.getMessage("AttributeType.name"));

		displayedName = new LocalizedTextFieldDetails(new HashSet<>(msg.getEnabledLocales()
				.values()), msg.getLocale());
		displayedName.setWidth(TEXT_FIELD_BIG.value());
		addFormItem(displayedName, msg.getMessage("AttributeType.displayedName"));

		typeDescription = new LocalizedTextAreaDetails(new HashSet<>(msg.getEnabledLocales()
				.values()), msg.getLocale());
		typeDescription.setWidthFull();
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
		ret.setName(name.getValue());
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
