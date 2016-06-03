/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.awt.image.BufferedImage;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.base.attributes.AttributeValueSyntaxFactory;

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
