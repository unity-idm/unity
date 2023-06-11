/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.base.attribute.Attribute;

/**
 * Helper class allowing to create datetime attributes easily.
 * 
 * @author P.Piernik
 *
 */
public class DateTimeAttribute
{
	public static Attribute of(String name, String groupPath, LocalDateTime... values)
	{
		return new Attribute(name, DateTimeAttributeSyntax.ID, groupPath, convert(values));
	}

	private static List<String> convert(LocalDateTime... values)
	{
		DateTimeAttributeSyntax syntax = new DateTimeAttributeSyntax();
		return Stream.of(values).map(v -> syntax.convertToString(v))
				.collect(Collectors.toList());
	}
}
