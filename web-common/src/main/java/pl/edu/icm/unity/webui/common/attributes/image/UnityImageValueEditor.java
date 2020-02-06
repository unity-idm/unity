/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.UnityImage;
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
	private boolean required;
	private BaseImageAttributeSyntax<UnityImage> syntax;
	private UnityImageValueComponent imageValueComponent;

	UnityImageValueEditor(String valueRaw,
			String label,
			UnityMessageSource msg,
			BaseImageAttributeSyntax<UnityImage> syntax)
	{
		UnityImage value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
		imageValueComponent = new UnityImageValueComponent(label, value, syntax.getConfig(), msg);
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
		UnityImage value = imageValueComponent.getUnityImageValue(required, syntax::validate);
		return syntax.convertToString(value);
	}

	@Override
	public void setLabel(String label)
	{
		imageValueComponent.setCaption(label);
	}
}
