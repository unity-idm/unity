/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.util.Optional;

import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Editor for Image values.
 *
 * @author R. Ledzinski
 */
class UnityImageValueEditor implements AttributeValueEditor
{
	private final BaseImageAttributeSyntax<UnityImage> syntax;
	private final UnityImageValueComponent imageValueComponent;
	private boolean required;

	UnityImageValueEditor(String valueRaw,
			String label,
			UnityMessageSource msg,
			BaseImageAttributeSyntax<UnityImage> syntax)
	{
		UnityImage value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
		imageValueComponent = new UnityImageValueComponent(value, syntax.getConfig(), msg);
		imageValueComponent.setCaption(label);
		this.syntax = syntax;
	}

	@Override
	public ComponentsContainer getEditor(AttributeEditContext context)
	{
		required = context.isRequired();
		return new ComponentsContainer(imageValueComponent);
	}
	
	@Override
	public String getCurrentValue() throws IllegalAttributeValueException
	{
		Optional<UnityImage> value = imageValueComponent.getValue(required, syntax::validate);
		return value.map(image -> syntax.convertToString(image)).orElse(null);
	}

	@Override
	public void setLabel(String label)
	{
		imageValueComponent.setCaption(label);
	}
}
