/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

/**
 * Extracts a plain-text, search-friendly representation of an attribute's values, to be stored alongside
 * the materialized attributes cache for substring search (e.g. directory browser quick search). Storage
 * itself has no notion of attribute value syntax plugins (that is an engine-level concept), so this
 * extraction is delegated through this interface to an engine-provided implementation.
 */
public interface AttributeSearchableValueExtractor
{
	/**
	 * @return a plain-text representation of the given attribute values suitable for substring search,
	 * or {@code null} if values of this syntax should not be searchable (e.g. binary/image syntaxes) or
	 * the value list is empty.
	 */
	String getSearchableValue(String valueSyntaxId, List<String> values);
}
