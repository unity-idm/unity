/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import java.util.List;

record MemberAttribute(String name, List<String> values)
{
	MemberAttribute
	{
		values = List.copyOf(values);
	}
}
