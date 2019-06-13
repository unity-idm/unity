/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.authproxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAttributes
{
	private Map<String, List<String>> attributes;
	
	public UserAttributes(Map<String, List<String>> attributes)
	{
		this.attributes = new HashMap<>(attributes);
	}
	
	public Map<String, List<String>> getAttributes()
	{
		return attributes;
	}

	@Override
	public String toString()
	{
		return "User attributes: " + attributes;
	}
}
