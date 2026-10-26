/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import static io.imunity.console.views.directory_browser.identities.IdentityEntryFixtures.entityRow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.imunity.console.views.directory_browser.identities.AddFilterDialog.Operand;
import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ColumnFilterFactoryTest
{
	private static final String CURRENT_DEPARTMENT = IdentitiesGridColumnConstants.ATTR_CURRENT_COL_PREFIX + "department";
	private static final String ROOT_DEPARTMENT = IdentitiesGridColumnConstants.ATTR_ROOT_COL_PREFIX + "department";
	private static final long SALES = 1L;
	private static final long ALICE_IN_DEPARTMENT = 2L;
	private static final long NO_DEPARTMENT = 3L;

	@Mock
	private AttributeSearchService searchService;

	private final Map<Long, String> displayedDepartment = new HashMap<>(Map.of(
			SALES, "sales",
			ALICE_IN_DEPARTMENT, "alice",
			NO_DEPARTMENT, "undefined"));

	private ColumnFilterFactory factory()
	{
		return new ColumnFilterFactory(searchService, (entityId, columnId) -> displayedDepartment.get(entityId));
	}

	@Test
	void containMatchesOnlyEntitiesWithTermInSelectedAttribute()
	{
		// entity SALES has "alice" in another attribute (e.g. email) - the scoped search doesn't return it
		when(searchService.searchEntities("/A", "department", "alice")).thenReturn(Set.of(ALICE_IN_DEPARTMENT));

		EntityFilter filter = factory().create(CURRENT_DEPARTMENT, Operand.contain, "alice", "/A");

		assertThat(filter.test(entityRow(SALES))).isFalse();
		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).isTrue();
		assertThat(filter.test(entityRow(NO_DEPARTMENT))).isFalse();
	}

	@Test
	void notContainKeepsEntitiesWithTermOnlyInOtherAttributesAndWithoutValue()
	{
		when(searchService.searchEntities("/A", "department", "alice")).thenReturn(Set.of(ALICE_IN_DEPARTMENT));

		EntityFilter filter = factory().create(CURRENT_DEPARTMENT, Operand.notContain, "alice", "/A");

		assertThat(filter.test(entityRow(SALES))).isTrue();
		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).isFalse();
		assertThat(filter.test(entityRow(NO_DEPARTMENT))).isTrue();
	}

	@Test
	void equalRequiresExactValueOfSelectedColumn()
	{
		// DB search is a case insensitive substring match, so it can return a superset
		when(searchService.searchEntities("/A", "department", "Alice"))
				.thenReturn(Set.of(SALES, ALICE_IN_DEPARTMENT));

		EntityFilter filter = factory().create(CURRENT_DEPARTMENT, Operand.equal, "Alice", "/A");

		assertThat(filter.test(entityRow(SALES))).isFalse();
		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).as("equal is case sensitive").isFalse();
		displayedDepartment.put(ALICE_IN_DEPARTMENT, "Alice");
		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).isTrue();
	}

	@Test
	void notEqualNegatesExactMatch()
	{
		when(searchService.searchEntities("/A", "department", "alice")).thenReturn(Set.of(ALICE_IN_DEPARTMENT));

		EntityFilter filter = factory().create(CURRENT_DEPARTMENT, Operand.notEqual, "alice", "/A");

		assertThat(filter.test(entityRow(SALES))).isTrue();
		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).isFalse();
		assertThat(filter.test(entityRow(NO_DEPARTMENT))).isTrue();
	}

	@Test
	void rootColumnIsSearchedInRootGroupRegardlessOfBrowsedGroup()
	{
		when(searchService.searchEntities("/", "department", "alice")).thenReturn(Set.of(ALICE_IN_DEPARTMENT));

		EntityFilter filter = factory().create(ROOT_DEPARTMENT, Operand.contain, "alice", "/A");

		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).isTrue();
		verify(searchService, never()).searchEntities(eq("/A"), anyString(), anyString());
	}

	@Test
	void notRenderedRowIsMatchedWithoutInMemoryValues()
	{
		when(searchService.searchEntities("/A", "department", "alice")).thenReturn(Set.of(ALICE_IN_DEPARTMENT));
		IdentityEntry notRendered = entityRow(ALICE_IN_DEPARTMENT);

		EntityFilter filter = factory().create(CURRENT_DEPARTMENT, Operand.contain, "alice", "/A");

		assertThat(notRendered.getAttribute(CURRENT_DEPARTMENT)).isNull();
		assertThat(filter.test(notRendered)).isTrue();
	}

	@Test
	void dbMatchesAreRecomputedOnRefresh()
	{
		when(searchService.searchEntities("/A", "department", "alice"))
				.thenReturn(Set.of(ALICE_IN_DEPARTMENT))
				.thenReturn(Set.of(SALES));
		EntityFilter filter = factory().create(CURRENT_DEPARTMENT, Operand.contain, "alice", "/A");

		filter.refresh("/A");

		assertThat(filter.test(entityRow(SALES))).isTrue();
		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).isFalse();
	}

	@Test
	void currentGroupColumnIsSearchedInNewGroupAfterGroupChange()
	{
		when(searchService.searchEntities("/A", "department", "alice")).thenReturn(Set.of(ALICE_IN_DEPARTMENT));
		when(searchService.searchEntities("/B", "department", "alice")).thenReturn(Set.of(SALES));
		EntityFilter filter = factory().create(CURRENT_DEPARTMENT, Operand.contain, "alice", "/A");

		filter.refresh("/B");

		assertThat(filter.test(entityRow(SALES))).isTrue();
		assertThat(filter.test(entityRow(ALICE_IN_DEPARTMENT))).isFalse();
	}

	@Test
	void baseColumnContainIgnoresCaseAndEqualDoesNot()
	{
		IdentityEntry row = entityRow(SALES);

		assertThat(factory().create("credReq", Operand.contain, "CREDREQ", "/A").test(row)).isTrue();
		assertThat(factory().create("credReq", Operand.notContain, "CREDREQ", "/A").test(row)).isFalse();
		assertThat(factory().create("credReq", Operand.equal, "credReq", "/A").test(row)).isTrue();
		assertThat(factory().create("credReq", Operand.equal, "CREDREQ", "/A").test(row)).isFalse();
		assertThat(factory().create("credReq", Operand.notEqual, "CREDREQ", "/A").test(row)).isTrue();
		verify(searchService, never()).searchEntities(anyString(), anyString(), anyString());
	}
}
