/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import static io.imunity.console.views.directory_browser.identities.IdentityEntryFixtures.entityRow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;

@ExtendWith(MockitoExtension.class)
class QuickSearchTest
{
	@Mock
	private AttributeSearchService searchService;

	private final List<EntityFilter> activeFilters = new ArrayList<>();
	private String groupPath = "/A";

	private QuickSearch quickSearch()
	{
		return new QuickSearch(searchService, () -> groupPath, () -> Set.of("credReq"), activeFilters::add,
				activeFilters::remove);
	}

	@Test
	void combinesDbMatchesOfRootAndCurrentGroupWithInMemoryColumnMatches()
	{
		when(searchService.searchEntities("/", "term")).thenReturn(Set.of(1L));
		when(searchService.searchEntities("/A", "term")).thenReturn(Set.of(2L));

		quickSearch().search("term");

		EntityFilter filter = activeFilters.get(0);
		assertThat(filter.test(entityRow(1L))).isTrue();
		assertThat(filter.test(entityRow(2L))).isTrue();
		assertThat(filter.test(entityRow(3L))).isFalse();
	}

	@Test
	void matchesVisibleBaseColumnWithoutDbMatch()
	{
		when(searchService.searchEntities(anyString(), anyString())).thenReturn(Set.of());

		quickSearch().search("CREDREQ");

		assertThat(activeFilters.get(0).test(entityRow(3L))).isTrue();
	}

	@Test
	void notRenderedRowIsMatchedByDb()
	{
		when(searchService.searchEntities(anyString(), anyString())).thenReturn(Set.of(5L));
		IdentityEntry notRendered = entityRow(5L);

		quickSearch().search("value-of-not-loaded-attribute");

		assertThat(activeFilters.get(0).test(notRendered)).isTrue();
	}

	@Test
	void rootGroupIsQueriedOnceWhenBrowsingRoot()
	{
		groupPath = "/";
		when(searchService.searchEntities("/", "term")).thenReturn(Set.of(1L));

		quickSearch().search("term");

		verify(searchService, times(1)).searchEntities(anyString(), anyString());
		assertThat(activeFilters.get(0).test(entityRow(1L))).isTrue();
	}

	@Test
	void clearingSearchRemovesFilterRestoringAllRows()
	{
		when(searchService.searchEntities(anyString(), anyString())).thenReturn(Set.of());
		QuickSearch quickSearch = quickSearch();
		quickSearch.search("term");
		assertThat(activeFilters).hasSize(1);

		quickSearch.search("");

		assertThat(activeFilters).isEmpty();
	}

	@Test
	void newSearchReplacesPreviousFilter()
	{
		when(searchService.searchEntities(anyString(), anyString())).thenReturn(Set.of());
		QuickSearch quickSearch = quickSearch();
		quickSearch.search("first");

		quickSearch.search("second");

		assertThat(activeFilters).hasSize(1);
		verify(searchService).searchEntities("/A", "second");
	}
}
