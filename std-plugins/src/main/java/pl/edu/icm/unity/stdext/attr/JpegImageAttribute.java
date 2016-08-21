/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create jpeg attributes easily.
 * @author K. Benedyczak
 */
public class JpegImageAttribute extends Attribute
{
	public JpegImageAttribute(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		super(name, JpegImageAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public JpegImageAttribute(String name, String groupPath, List<BufferedImage> values)
	{
		super(name, JpegImageAttributeSyntax.ID, groupPath, convert(values));
	}
	
	public JpegImageAttribute(String name, String groupPath, BufferedImage... values)
	{
		this(name, groupPath, Lists.newArrayList(values));
	}	
	
	private static List<String> convert(List<BufferedImage> values)
	{
		JpegImageAttributeSyntax syntax = new JpegImageAttributeSyntax();
		return values.stream().
				map(v -> syntax.convertToString(v)).
				collect(Collectors.toList());
	}
}
