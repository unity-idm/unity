/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;

public class AttributeFilterTest
{
	@Test
	public void shouldFilterAttrsByValues()
	{
		List<AttributeFilteringSpec> filters = List.of(new AttributeFilteringSpec("a", Set.of("a1", "a2")),
				new AttributeFilteringSpec("b", Set.of("b1", "b2")), new AttributeFilteringSpec("c", Set.of("c1")));

		List<DynamicAttribute> attributes = List.of(
				new DynamicAttribute(new Attribute("a", null, null, List.of("a1", "a3"))),
				new DynamicAttribute(new Attribute("b", null, null, List.of("b1", "b2"))),
				new DynamicAttribute(new Attribute("c", null, null, List.of("c2"))),
				new DynamicAttribute(new Attribute("d", null, null, List.of("d1", "d2"))));

		Set<DynamicAttribute> filteredAttributes = AttributeValueFilter.filterAttributes(filters, attributes);

		assertThat(filteredAttributes).containsExactlyInAnyOrder(
				new DynamicAttribute(new Attribute("a", null, null, List.of("a1"))),
				new DynamicAttribute(new Attribute("b", null, null, List.of("b1", "b2"))),
				new DynamicAttribute(new Attribute("d", null, null, List.of("d1", "d2"))));
	}
}
