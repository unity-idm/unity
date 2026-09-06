/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Optional;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Self-validates the materialized attributes cache: freshly generates effective attributes for all
 * members of the given (or all) group(s) and compares them with what is stored in the cache.
 */
public interface AttributeCacheValidationService
{
	/**
	 * @param group group to validate, or empty to validate all groups.
	 */
	AttributeCacheValidationReport validate(Optional<String> group) throws EngineException;
}
