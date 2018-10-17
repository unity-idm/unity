/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create zoned datetime attributes easily.
 * 
 * @author P.Piernik
 *
 */
public class ZonedDateTimeAttribute
{
	public static Attribute of(String name, String groupPath, ZonedDateTime... values)
	{
		return new Attribute(name, ZonedDateTimeAttributeSyntax.ID, groupPath, convert(values));
	}

	private static List<String> convert(ZonedDateTime... values)
	{
		ZonedDateTimeAttributeSyntax syntax = new ZonedDateTimeAttributeSyntax();
		return Stream.of(values).map(v -> syntax.convertToString(v))
				.collect(Collectors.toList());
	}
}
