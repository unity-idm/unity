/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.engine.api.attributes.CachedAttributes;
import pl.edu.icm.unity.engine.api.attributes.EffectiveAttributesCacheService;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@ExtendWith(MockitoExtension.class)
class EntityAttributesLookupTest
{
	private static final String ROOT_NAME = IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX + "name";
	private static final String CURRENT_NAME = IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX + "name";
	private static final String CURRENT_EMAIL = IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX + "email";

	@Mock
	private EffectiveAttributesCacheService cacheService;

	@Test
	void rootAndCurrentColumnsAreReadFromTheirGroups()
	{
		stubAttributes(1L, "/", "name", "in-root");
		stubAttributes(1L, "/A", "name", "in-A");
		EntityAttributesLookup lookup = new EntityAttributesLookup(cacheService);
		lookup.reset("/A");

		assertThat(lookup.getColumnAttribute(1L, ROOT_NAME).getValues()).containsExactly("in-root");
		assertThat(lookup.getColumnAttribute(1L, CURRENT_NAME).getValues()).containsExactly("in-A");
	}

	@Test
	void currentColumnInRootGroupReusesRootLookup()
	{
		stubAttributes(1L, "/", "name", "in-root");
		EntityAttributesLookup lookup = new EntityAttributesLookup(cacheService);
		lookup.reset("/");

		lookup.getColumnAttribute(1L, ROOT_NAME);
		assertThat(lookup.getColumnAttribute(1L, CURRENT_NAME).getValues()).containsExactly("in-root");

		verify(cacheService, times(1)).getAttributesFast(1L, "/");
	}

	@Test
	void entityIsQueriedOnceForAllItsRowsAndColumns()
	{
		when(cacheService.getAttributesFast(1L, "/A")).thenReturn(new CachedAttributes(Map.of(
				"name", attribute("name", "/A", "n"),
				"email", attribute("email", "/A", "e")), false));
		EntityAttributesLookup lookup = new EntityAttributesLookup(cacheService);
		lookup.reset("/A");

		// two identity rows of the same entity, then a column added after the initial display
		lookup.getColumnAttribute(1L, CURRENT_NAME);
		lookup.getColumnAttribute(1L, CURRENT_NAME);
		assertThat(lookup.getColumnAttribute(1L, CURRENT_EMAIL).getValues()).containsExactly("e");

		verify(cacheService, times(1)).getAttributesFast(1L, "/A");
	}

	@Test
	void missingAttributeIsReturnedAsNull()
	{
		stubAttributes(1L, "/A", "name", "n");
		EntityAttributesLookup lookup = new EntityAttributesLookup(cacheService);
		lookup.reset("/A");

		assertThat(lookup.getColumnAttribute(1L, CURRENT_EMAIL)).isNull();
	}

	@Test
	void resetClearsCachedValuesAndPendingStateAndSwitchesGroup()
	{
		stubAttributes(1L, "/A", "name", "in-A");
		stubAttributes(1L, "/B", "name", "in-B");
		when(cacheService.isUpdatePending(1L, "/A")).thenReturn(true);
		when(cacheService.isUpdatePending(1L, "/B")).thenReturn(false);
		EntityAttributesLookup lookup = new EntityAttributesLookup(cacheService);
		lookup.reset("/A");
		assertThat(lookup.isPending(1L)).isTrue();
		assertThat(lookup.getColumnAttribute(1L, CURRENT_NAME).getValues()).containsExactly("in-A");

		lookup.reset("/B");

		assertThat(lookup.isPending(1L)).isFalse();
		assertThat(lookup.getColumnAttribute(1L, CURRENT_NAME).getValues()).containsExactly("in-B");
	}

	@Test
	void reloadOfSameGroupQueriesAgain()
	{
		when(cacheService.isUpdatePending(1L, "/A")).thenReturn(true, false);
		EntityAttributesLookup lookup = new EntityAttributesLookup(cacheService);
		lookup.reset("/A");
		assertThat(lookup.isPending(1L)).isTrue();
		assertThat(lookup.isPending(1L)).as("cached until reset").isTrue();

		lookup.reset("/A");

		assertThat(lookup.isPending(1L)).isFalse();
	}

	private void stubAttributes(long entityId, String group, String name, String value)
	{
		when(cacheService.getAttributesFast(entityId, group))
				.thenReturn(new CachedAttributes(Map.of(name, attribute(name, group, value)), false));
	}

	private static AttributeExt attribute(String name, String group, String value)
	{
		return new AttributeExt(StringAttribute.of(name, group, value), true);
	}
}
