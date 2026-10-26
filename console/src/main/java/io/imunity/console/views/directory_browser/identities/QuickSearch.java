/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;

/**
 * Maintains the identities grid filter of the quick (free text) search: matches the visible base and
 * already rendered columns in memory, and additionally attribute values in the DB - as attribute values
 * are no longer bulk pre-loaded (UY-1483), so rows not yet rendered hold no attribute values in memory.
 */
class QuickSearch
{
	private final AttributeSearchService attributeSearchService;
	private final Supplier<String> groupPath;
	private final Supplier<Set<String>> visibleColumns;
	private final Consumer<EntityFilter> filterAdder;
	private final Consumer<EntityFilter> filterRemover;
	private EntityFilter currentFilter;

	QuickSearch(AttributeSearchService attributeSearchService, Supplier<String> groupPath,
			Supplier<Set<String>> visibleColumns, Consumer<EntityFilter> filterAdder,
			Consumer<EntityFilter> filterRemover)
	{
		this.attributeSearchService = attributeSearchService;
		this.groupPath = groupPath;
		this.visibleColumns = visibleColumns;
		this.filterAdder = filterAdder;
		this.filterRemover = filterRemover;
	}

	void search(String term)
	{
		if (currentFilter != null)
		{
			filterRemover.accept(currentFilter);
			currentFilter = null;
		}
		if (term == null || term.isEmpty())
			return;
		Set<Long> dbAttributeMatches = searchAttributeValuesInDb(term);
		currentFilter = e -> e.anyFieldContains(term, visibleColumns.get())
				|| dbAttributeMatches.contains(e.getSourceEntity().getEntity().getId());
		filterAdder.accept(currentFilter);
	}

	private Set<Long> searchAttributeValuesInDb(String term)
	{
		Set<Long> rootMatches = attributeSearchService.searchEntities("/", term);
		String group = groupPath.get();
		if ("/".equals(group))
			return rootMatches;
		return Sets.union(rootMatches, attributeSearchService.searchEntities(group, term));
	}
}
