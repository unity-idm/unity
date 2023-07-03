/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.image.LinkableImage;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Editor for Image values.
 *
 * @author R. Ledzinski
 */
class PublicLinkableImageValueEditor implements AttributeValueEditor
{
	private final PublicLinkableImageSyntax syntax;
	private final PublicLinkableImageValueComponent valueComponent;
	private boolean required;

	PublicLinkableImageValueEditor(String valueRaw,
			String label,
			MessageSource msg,
			PublicLinkableImageSyntax syntax)
	{
		LinkableImage value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
		valueComponent = new PublicLinkableImageValueComponent(value, syntax.getConfig(), msg);
		valueComponent.setCaption(label);
		this.syntax = syntax;
	}

	@Override
	public ComponentsContainer getEditor(AttributeEditContext context)
	{
		required = context.isRequired();
		return new ComponentsContainer(valueComponent);
	}

	@Override
	public String getCurrentValue() throws IllegalAttributeValueException
	{
		return valueComponent.getValue(required, syntax)
				.map(value -> syntax.convertToString(value))
				.orElse(null);
	}

	@Override
	public void setLabel(String label)
	{
		valueComponent.setCaption(label);
	}
}
