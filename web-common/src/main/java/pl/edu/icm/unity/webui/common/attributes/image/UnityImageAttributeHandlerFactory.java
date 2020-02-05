/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

/**
 * Factory class to instantiate ImageAttributeHandler
 *
 * @author R. Ledzinski
 */
@Component
class UnityImageAttributeHandlerFactory implements WebAttributeHandlerFactory
{
	private final UnityMessageSource msg;

	@Autowired
	UnityImageAttributeHandlerFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return ImageAttributeSyntax.ID;
	}

	@Override
	public AttributeSyntaxEditor<UnityImage> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
	{
		return new BaseImageSyntaxEditor<>((ImageAttributeSyntax) initialValue, ImageAttributeSyntax::new, msg);
	}

	@Override
	public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
	{
		return new BaseImageAttributeHandler<>(msg, (ImageAttributeSyntax) syntax);
	}
}
