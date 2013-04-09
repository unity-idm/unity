/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Helper class allowing to create jpeg attributes easily.
 * @author K. Benedyczak
 */
public class JpegImageAttribute extends Attribute<BufferedImage>
{
	public JpegImageAttribute(String name, String groupPath, AttributeVisibility visibility,
			List<BufferedImage> values)
	{
		super(name, new JpegImageAttributeSyntax(), groupPath, visibility, values);
	}

	public JpegImageAttribute(String name, String groupPath, AttributeVisibility visibility,
			BufferedImage value)
	{
		this(name, groupPath, visibility, Collections.singletonList(value));
	}
	
	public JpegImageAttribute()
	{
	}
}
