/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.bulkops;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum EntityMVELContextKey
{	
	idsByType(EntityMVELContextKey.descriptionPrefix + "idsByType"),
	idsByTypeObj(EntityMVELContextKey.descriptionPrefix + "idsByTypeObj"),
	attrs(EntityMVELContextKey.descriptionPrefix + "attrs"),
	attr(EntityMVELContextKey.descriptionPrefix + "attr"),
	groups(EntityMVELContextKey.descriptionPrefix + "groups"),
	status(EntityMVELContextKey.descriptionPrefix + "status"),
	credReq(EntityMVELContextKey.descriptionPrefix + "credReq"),
	credStatus(EntityMVELContextKey.descriptionPrefix + "credStatus");

	public static final String descriptionPrefix = "EntityMVELContextKey.";
	public final String descriptionKey;

	private EntityMVELContextKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}


	public static Map<String, String> toMap()
	{
		return Stream.of(values()).collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}
}