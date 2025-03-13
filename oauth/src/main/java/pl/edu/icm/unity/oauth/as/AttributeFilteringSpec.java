/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Set;

public record AttributeFilteringSpec(
		String attributeName,
		Set<String> values)
{
}
