/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.LinkableImage;
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
	private PublicLinkableImageSyntax syntax;
	private PublicLinkableImageValueComponent valueComponent;

	PublicLinkableImageValueEditor(String valueRaw,
			String label,
			UnityMessageSource msg,
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
		valueComponent.setValueRequired(context.isRequired());
		return new ComponentsContainer(valueComponent);
	}

	@Override
	public String getCurrentValue() throws IllegalAttributeValueException
	{
		LinkableImage value = valueComponent.getValue(syntax);
		return syntax.convertToString(value);
	}

	@Override
	public void setLabel(String label)
	{
		valueComponent.setCaption(label);
	}
}
