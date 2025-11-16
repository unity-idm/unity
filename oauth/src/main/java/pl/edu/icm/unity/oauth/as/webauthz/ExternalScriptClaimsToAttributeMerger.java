/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.oauth.as.webauthz.externalScript.Claim;
import pl.edu.icm.unity.stdext.attr.JsonAttribute;
import pl.edu.icm.unity.stdext.attr.JsonAttributeSyntax;

class ExternalScriptClaimsToAttributeMerger
{
	static Collection<DynamicAttribute> mergeClaimsWithAttributes(Collection<DynamicAttribute> attributes,
			List<Claim> claims)
	{
		Map<String, DynamicAttribute> mappedAttributes = attributes.stream()
				.collect(Collectors.toMap(a -> a.getAttribute()
						.getName(), a -> a));

		if (claims == null || claims.isEmpty())
			return mappedAttributes.values();

		for (Claim claim : claims)
		{
			if (mappedAttributes.containsKey(claim.name()))
				mappedAttributes.remove(claim.name());
			mappedAttributes.put(claim.name(), new DynamicAttribute(JsonAttribute.of(claim.name(), "/", claim.values()),
					new AttributeType(claim.name(), JsonAttributeSyntax.ID), claim.name(), "", false));
		}
		return mappedAttributes.values();
	}
}
