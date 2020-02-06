/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.LinkableImage;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

class PublicLinkableImageAttributeHandler implements WebAttributeHandler
{
	private UnityMessageSource msg;
	private PublicLinkableImageSyntax syntax;

	PublicLinkableImageAttributeHandler(UnityMessageSource msg, PublicLinkableImageSyntax syntax)
	{
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public String getValueAsString(String value)
	{
		return PublicLinkableImageSyntax.ID;
	}

	@Override
	public Component getRepresentation(String valueRaw, AttributeViewerContext context)
	{
		LinkableImage value = syntax.convertFromString(valueRaw);
		return new ImageRepresentationComponent(value.getUnityImage(), context);
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new PublicLinkableImageValueEditor(initialValue, label, msg, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new CompactFormLayout(UnityImageValueComponent.getHints(syntax.getConfig(), msg));
	}
}
