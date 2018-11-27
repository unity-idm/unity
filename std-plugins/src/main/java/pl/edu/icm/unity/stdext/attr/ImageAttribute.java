/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import com.google.common.collect.Lists;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.types.basic.Attribute;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class allowing to create image attributes easily.
 *
 * @author R. Ledzinski
 */
public class ImageAttribute
{
	public static Attribute of(String name, String groupPath,
							   List<String> values, String remoteIdp, String translationProfile)
	{
		return new Attribute(name, ImageAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<UnityImage> values)
	{
		return new Attribute(name, ImageAttributeSyntax.ID, groupPath, convert(values));
	}

	public static Attribute of(String name, String groupPath, UnityImage... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}

	private static List<String> convert(List<UnityImage> values)
	{
		ImageAttributeSyntax syntax = new ImageAttributeSyntax();
		return values.stream().
				map(v -> syntax.convertToString(v)).
				collect(Collectors.toList());
	}
}
