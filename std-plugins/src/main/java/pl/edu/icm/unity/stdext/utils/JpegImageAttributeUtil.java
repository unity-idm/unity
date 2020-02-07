/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.attr.ImageType;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create jpeg attributes easily.
 * 
 * @author K. Benedyczak
 */
public final class JpegImageAttributeUtil
{
	public static Attribute of(String name,
			String groupPath,
			List<String> values,
			String remoteIdp,
			String translationProfile)
	{
		return new Attribute(name, JpegImageAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<BufferedImage> values)
	{
		return new Attribute(name, JpegImageAttributeSyntax.ID, groupPath, convert(values));
	}

	public static Attribute of(String name, String groupPath, BufferedImage... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}

	private static List<String> convert(List<BufferedImage> values)
	{
		JpegImageAttributeSyntax syntax = new JpegImageAttributeSyntax();
		return values.stream()
				.map(bufferedImage -> new UnityImage(bufferedImage, ImageType.JPG))
				.map(v -> syntax.convertToString(v))
				.collect(Collectors.toList());
	}
}
