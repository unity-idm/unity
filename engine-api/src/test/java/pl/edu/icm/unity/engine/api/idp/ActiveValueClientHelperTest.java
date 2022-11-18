/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import org.junit.Test;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ActiveValueClientHelperTest
{
	@Test
	public void shouldReturnFilteredAndRemainingAttributes()
	{
		ActiveValueClient activeValueClient = new ActiveValueClient("1", "client", List.of("a1"), List.of("a2"));

		Collection<DynamicAttribute> allAttributes = List.of(
				new DynamicAttribute(new Attribute("a1", "syntax", "/", List.of("v1"))),
				new DynamicAttribute(new Attribute("a2", "syntax", "/", List.of("v1"))),
				new DynamicAttribute(new Attribute("a3", "syntax", "/", List.of("v1")))
				);


		Optional<ActiveValueClientHelper.ActiveValueSelectionConfig> selectionConfig = ActiveValueClientHelper.getActiveValueSelectionConfig(
				Set.of(activeValueClient), "client", allAttributes);

		assertThat(selectionConfig).isPresent();
		assertThat(selectionConfig.get().multiSelectableAttributes.size()).isEqualTo(1);
		assertThat(selectionConfig.get().multiSelectableAttributes.get(0).getAttribute().getName()).isEqualTo("a2");

		assertThat(selectionConfig.get().singleSelectableAttributes.size()).isEqualTo(1);
		assertThat(selectionConfig.get().singleSelectableAttributes.get(0).getAttribute().getName()).isEqualTo("a1");

		assertThat(selectionConfig.get().remainingAttributes.size()).isEqualTo(1);
		assertThat(selectionConfig.get().remainingAttributes.get(0).getAttribute().getName()).isEqualTo("a3");
	}
}
