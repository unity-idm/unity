/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attr.LinkableImage;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

@Component
class PublicLinkableImageAttributeHandlerFactory implements WebAttributeHandlerFactory
{
	private final MessageSource msg;
	private final URL serverAdvertisedAddress;

	@Autowired
	PublicLinkableImageAttributeHandlerFactory(MessageSource msg,
			AdvertisedAddressProvider advertisedAddressProvider)
	{
		this.msg = msg;
		this.serverAdvertisedAddress = advertisedAddressProvider.get();
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return PublicLinkableImageSyntax.ID;
	}

	@Override
	public AttributeSyntaxEditor<LinkableImage> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
	{
		return new BaseImageSyntaxEditor<>((PublicLinkableImageSyntax) initialValue,
				() -> new PublicLinkableImageSyntax(serverAdvertisedAddress), msg);
	}

	@Override
	public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
	{
		return new PublicLinkableImageAttributeHandler(msg, (PublicLinkableImageSyntax) syntax);
	}
}
