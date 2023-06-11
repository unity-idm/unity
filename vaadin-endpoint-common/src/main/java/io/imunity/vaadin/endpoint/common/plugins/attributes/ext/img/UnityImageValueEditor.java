/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeValueEditor;
import pl.edu.icm.unity.base.attr.UnityImage;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;

import java.util.Optional;


class UnityImageValueEditor implements AttributeValueEditor
{
	private final BaseImageAttributeSyntax<UnityImage> syntax;
	private final UnityImageValueComponent imageValueComponent;
	private boolean required;

	UnityImageValueEditor(String valueRaw,
	                      String label,
	                      MessageSource msg,
	                      BaseImageAttributeSyntax<UnityImage> syntax)
	{
		UnityImage value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
		imageValueComponent = new UnityImageValueComponent(value, syntax.getConfig(), msg);
		imageValueComponent.setLabel(label);
		this.syntax = syntax;
	}

	@Override
	public ComponentsContainer getEditor(AttributeEditContext context)
	{
		required = context.isRequired();
		imageValueComponent.setRequired(required);
		return new ComponentsContainer(imageValueComponent);
	}
	
	@Override
	public String getCurrentValue() throws IllegalAttributeValueException
	{
		Optional<UnityImage> value = imageValueComponent.getValue(required, syntax::validate);
		return value.map(syntax::convertToString).orElse(null);
	}

	@Override
	public void setLabel(String label)
	{
		imageValueComponent.setLabel(label);
	}
}
