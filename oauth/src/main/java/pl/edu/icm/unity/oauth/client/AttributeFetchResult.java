/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeFetchResult
{
	private Map<String, List<String>> attributes;
	private Map<String, Object> rawAttributes;
	
	public AttributeFetchResult(Map<String, List<String>> attributes,
			Map<String, Object> rawAttributes)
	{
		this.attributes = new HashMap<>(attributes);
		this.rawAttributes = new HashMap<>(rawAttributes);
	}
	
	public AttributeFetchResult()
	{
		attributes = new HashMap<>();
		rawAttributes = new HashMap<>();
	}

	public AttributeFetchResult mergeWith(AttributeFetchResult other)
	{
		Map<String, List<String>> attributes = new HashMap<>(this.attributes);
		Map<String, Object> rawAttributes = new HashMap<>(this.rawAttributes);
		attributes.putAll(other.getAttributes());
		rawAttributes.putAll(other.getRawAttributes());
		return new AttributeFetchResult(attributes, rawAttributes);
	}
	
	public Map<String, List<String>> getAttributes()
	{
		return attributes;
	}

	public Map<String, Object> getRawAttributes()
	{
		return rawAttributes;
	}

	@Override
	public String toString()
	{
		return "AttributeFetchResult [attributes=" + attributes + ", rawAttributes=" + rawAttributes + "]";
	}
}
