/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.attributes;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AttributeStatementMVELContextKey
{
	idsByType(AttributeStatementMVELContextKey.descriptionPrefix + "idsByType"),
	attrs(AttributeStatementMVELContextKey.descriptionPrefix + "attrs"),
	attr(AttributeStatementMVELContextKey.descriptionPrefix + "attr"),
	eattrs(AttributeStatementMVELContextKey.descriptionPrefix + "eattrs"),
	eattr(AttributeStatementMVELContextKey.descriptionPrefix + "eattr"),
	groupName(AttributeStatementMVELContextKey.descriptionPrefix + "groupName"),
	groups(AttributeStatementMVELContextKey.descriptionPrefix + "groups"),
	groupsObj(AttributeStatementMVELContextKey.descriptionPrefix + "groupsObj"),
	entityId(AttributeStatementMVELContextKey.descriptionPrefix + "entityId");

	public static final String descriptionPrefix = "AttributeStatementMVELContextKey.";
	public final String descriptionKey;

	private AttributeStatementMVELContextKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}
		
	public static Map<String, String> toMap()
	{
		return Stream.of(values()).collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));		
	}
	
}