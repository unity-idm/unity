/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public record ResolvedAuthenticationContextClassReference(List<String> essentialACRs, List<String> voluntaryACRs)
{
	public List<String> getAll()
	{
		return Stream.of(essentialACRs, voluntaryACRs).flatMap(Collection::stream).toList();
	}
}
