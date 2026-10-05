/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Set;

/**
 * Searches the materialized attributes cache (see {@link EffectiveAttributesCacheService}) for entities
 * having an attribute value containing a given search term. Backs the directory browser's quick search
 * and attribute-column filters, so attribute values do not need to be bulk pre-loaded to be searchable.
 */
public interface AttributeSearchService
{
	/**
	 * @return ids of entities having, in the given group, a searchable cached attribute value containing
	 * the given search term (case insensitive substring match). Binary attribute values (e.g. images) are
	 * never matched. Based on the cache, so an entity whose cache update is currently pending may be
	 * missed until the pending refresh completes.
	 * @throws pl.edu.icm.unity.engine.api.authn.AuthorizationExceptionRT if the caller lacks read/readHidden
	 * authorization for the given group
	 */
	Set<Long> searchEntities(String group, String searchTerm);
}
