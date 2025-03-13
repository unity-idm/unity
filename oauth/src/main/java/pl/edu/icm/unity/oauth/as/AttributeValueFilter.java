/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;

class AttributeValueFilter
{
	static Set<DynamicAttribute> filterAttributes(List<AttributeFilteringSpec> filter,
			Collection<DynamicAttribute> attributes)
	{
		if(filter == null)
			return attributes.stream().collect(Collectors.toSet());
		
		Set<DynamicAttribute> ret = new HashSet<>();

		Map<String, AttributeFilteringSpec> filtersByAttrName = filter.stream()
				.collect(Collectors.toMap(f -> f.attributeName(), f -> f));

		for (DynamicAttribute attribute : attributes)
		{
			if (filtersByAttrName.containsKey(attribute.getAttribute()
					.getName()))
			{
				List<String> filteredValues = attribute.getAttribute()
						.getValues()
						.stream()
						.filter(v -> filtersByAttrName.get(attribute.getAttribute()
								.getName())
								.values()
								.contains(v))
						.toList();
				if (filteredValues.isEmpty())
				{
					continue;
				}

				Attribute newAttribute = new Attribute(attribute.getAttribute());
				newAttribute.setValues(filteredValues);
				ret.add(new DynamicAttribute(newAttribute, attribute.getAttributeType(), attribute.getDisplayedName(),
						attribute.getDescription(), attribute.isMandatory()));

			} else
			{
				ret.add(attribute);
			}
		}

		return ret;
	}
}
