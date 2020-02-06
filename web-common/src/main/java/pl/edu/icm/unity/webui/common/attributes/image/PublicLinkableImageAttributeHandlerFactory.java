/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.LinkableImage;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

//@Component
class PublicLinkableImageAttributeHandlerFactory implements WebAttributeHandlerFactory
{
	private final UnityMessageSource msg;

	@Autowired
	PublicLinkableImageAttributeHandlerFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return PublicLinkableImageSyntax.ID;
	}

	@Override
	public AttributeSyntaxEditor<LinkableImage> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
	{
		return new BaseImageSyntaxEditor<>((PublicLinkableImageSyntax) initialValue, PublicLinkableImageSyntax::new, msg);
	}

	@Override
	public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
	{
//		return new BaseImageAttributeHandler(msg, (PublicLinkableImageSyntax) syntax);
		return null;
	}
}
