/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.attr;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;

public class JsonAttribute
{
	public static Attribute of(String name, String groupPath, List<JsonNode> values, String remoteIdp,
			String translationProfile)
	{
		return new Attribute(name, JsonAttributeSyntax.ID, groupPath, toString(values), remoteIdp, translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<JsonNode> values)
	{
		return new Attribute(name, JsonAttributeSyntax.ID, groupPath, toString(values));
	}

	public static Attribute of(String name, String groupPath, JsonNode... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}

	public static Attribute of(String name, String groupPath, String... values)
	{
		return of(name, groupPath, values);
	}

	private static List<String> toString(List<JsonNode> values)
	{
		return values.stream().map(JsonAttributeSyntax::toString)
				.collect(Collectors.toList());
	}
}
