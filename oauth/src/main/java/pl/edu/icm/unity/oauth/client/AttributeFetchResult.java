/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cotains attributes fetching result
 * @author P.Piernik
 *
 */
public class AttributeFetchResult
{

	private Map<String, List<String>> attributes;
	private Map<String, Object> rawAttributes;
	
	public AttributeFetchResult(Map<String, List<String>> attributes,
			Map<String, Object> rawAttributes)
	{
		this.setAttributes(attributes);
		this.setRawAttributes(rawAttributes);
	}
	
	public AttributeFetchResult()
	{
		attributes = new HashMap<>();
		rawAttributes = new HashMap<>();
	}

	public Map<String, List<String>> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(Map<String, List<String>> flatAttributes)
	{
		this.attributes = flatAttributes;
	}

	public Map<String, Object> getRawAttributes()
	{
		return rawAttributes;
	}

	public void setRawAttributes(Map<String, Object> rawAttributes)
	{
		this.rawAttributes = rawAttributes;
	}

	@Override
	public String toString()
	{
		return "AttributeFetchResult [attributes=" + attributes + ", rawAttributes=" + rawAttributes + "]";
	}
}
