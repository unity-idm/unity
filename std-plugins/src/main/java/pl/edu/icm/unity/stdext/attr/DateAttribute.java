/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create date attributes easily.
 * 
 * @author P.Piernik
 *
 */
public class DateAttribute
{
	public static Attribute of(String name, String groupPath, LocalDate... values)
	{
		return new Attribute(name, DateAttributeSyntax.ID, groupPath, convert(values));
	}

	private static List<String> convert(LocalDate... values)
	{
		DateAttributeSyntax syntax = new DateAttributeSyntax();
		return Stream.of(values).map(v -> syntax.convertToString(v))
				.collect(Collectors.toList());
	}
}
