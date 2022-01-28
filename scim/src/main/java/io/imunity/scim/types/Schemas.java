/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.types;

import java.util.Arrays;
import java.util.HashSet;

public class Schemas extends HashSet<String>
{
	public Schemas(String... schemas)
	{
		addAll(Arrays.asList(schemas));
	}

	public static Schemas of(String... schemas)
	{
		return new Schemas(schemas);
	}
}
