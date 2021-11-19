/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.translation.in;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum InputTranslationMVELContextKey
{
	id (InputTranslationMVELContextKey.descriptionPrefix + "id"), 
	idType (InputTranslationMVELContextKey.descriptionPrefix + "idType"),
	idsByType (InputTranslationMVELContextKey.descriptionPrefix + "idsByType"),
	attrs (InputTranslationMVELContextKey.descriptionPrefix + "attrs"),
	attr (InputTranslationMVELContextKey.descriptionPrefix + "attr"),
	attrObj (InputTranslationMVELContextKey.descriptionPrefix + "attrObj"),
	idp (InputTranslationMVELContextKey.descriptionPrefix + "idp"),
	groups (InputTranslationMVELContextKey.descriptionPrefix + "groups");

	public static final String descriptionPrefix = "InputTranslationMVELContextKey.";
	public final String descriptionKey;

	private InputTranslationMVELContextKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}

	public static Map<String, String> toMap()
	{
		return Stream.of(values()).collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}

}