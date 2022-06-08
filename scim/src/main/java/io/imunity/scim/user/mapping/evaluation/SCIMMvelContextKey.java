/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SCIMMvelContextKey
{
	idsByType(SCIMMvelContextKey.descriptionPrefix + "idsByType"),
	attrObj(SCIMMvelContextKey.descriptionPrefix + "attrObj"),
	groups(SCIMMvelContextKey.descriptionPrefix + "groups"),
	groupsObj(SCIMMvelContextKey.descriptionPrefix + "groupsObj"),
	arrayObj(SCIMMvelContextKey.descriptionPrefix + "arrayObj"),
	attr(SCIMMvelContextKey.descriptionPrefix + "attr"),
	attrs(SCIMMvelContextKey.descriptionPrefix + "attrs");
	
	public static final String descriptionPrefix = "SCIMContextKey.";
	public final String descriptionKey;

	private SCIMMvelContextKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}

	public static Map<String, String> mapForSingle()
	{
		return Stream.of(values()).filter(a -> !a.equals(arrayObj))
				.collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}

	public static Map<String, String> mapForMulti()
	{
		return Stream.of(values()).collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}
}
