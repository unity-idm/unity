/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

@Component
class JpegImageAttributeHandlerFactory implements WebAttributeHandlerFactory
{
	private final UnityMessageSource msg;

	@Autowired
	JpegImageAttributeHandlerFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return JpegImageAttributeSyntax.ID;
	}

	@Override
	public AttributeSyntaxEditor<UnityImage> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
	{
		return new BaseImageSyntaxEditor<>((JpegImageAttributeSyntax) initialValue, JpegImageAttributeSyntax::new, msg);
	}

	@Override
	public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
	{
		return new UnityImageAttributeHandler(msg, (JpegImageAttributeSyntax) syntax);
	}
}
