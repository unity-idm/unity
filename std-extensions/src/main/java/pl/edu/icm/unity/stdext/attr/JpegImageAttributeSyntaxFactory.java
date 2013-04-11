/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.awt.image.BufferedImage;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

@Component
public class JpegImageAttributeSyntaxFactory implements AttributeValueSyntaxFactory<BufferedImage>
{
	@Override
	public AttributeValueSyntax<BufferedImage> createInstance()
	{
		return new JpegImageAttributeSyntax();
	}

	@Override
	public String getId()
	{
		return JpegImageAttributeSyntax.ID;
	}
	
}
