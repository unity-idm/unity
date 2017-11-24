/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
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

	private Map<String, List<String>> flatAttributes;
	private Map<String, Object> rawAttributes;
	
	public AttributeFetchResult(Map<String, List<String>> flatAttributes,
			Map<String, Object> rawAttributes)
	{
		this.setFlatAttributes(flatAttributes);
		this.setRawAttributes(rawAttributes);
	}
	
	public AttributeFetchResult()
	{
		flatAttributes = new HashMap<>();
		rawAttributes = new HashMap<>();
	}

	public Map<String, List<String>> getFlatAttributes()
	{
		return flatAttributes;
	}

	public void setFlatAttributes(Map<String, List<String>> flatAttributes)
	{
		this.flatAttributes = flatAttributes;
	}

	public Map<String, Object> getRawAttributes()
	{
		return rawAttributes;
	}

	public void setRawAttributes(Map<String, Object> rawAttributes)
	{
		this.rawAttributes = rawAttributes;
	}	
}
