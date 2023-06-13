/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.attr.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Image attribute handler for the web
 *
 * @author R. Ledzinski
 */
class UnityImageAttributeHandler implements WebAttributeHandler
{
	private final MessageSource msg;
	private final BaseImageAttributeSyntax<UnityImage> syntax;

	UnityImageAttributeHandler(MessageSource msg, BaseImageAttributeSyntax<UnityImage> syntax)
	{
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public String getValueAsString(String value)
	{
		return "Image";
	}

	@Override
	public Component getRepresentation(String valueRaw, AttributeViewerContext context)
	{
		UnityImage value = syntax.convertFromString(valueRaw);
		return new ImageRepresentationComponent(value, context);
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new UnityImageValueEditor(initialValue, label, msg, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new CompactFormLayout(UnityImageValueComponent.getHints(syntax.getConfig(), msg));
	}
}
