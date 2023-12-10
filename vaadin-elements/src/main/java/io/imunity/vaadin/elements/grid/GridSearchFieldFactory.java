/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements.grid;

import java.util.function.Function;

import io.imunity.vaadin.elements.SearchField;

/**
 * Helps create search field for {@link FilterableGrid}
 * 
 * @author P.Piernik
 *
 */
public class GridSearchFieldFactory
{
	public static SearchField generateSearchField(FilterableGrid<? extends FilterableEntry> grid, Function<String, String> msg)
	{
		SearchField searchText = new SearchField(msg.apply("search"), searched ->
		{

			grid.clearFilters();
			if (searched == null || searched.isEmpty())
			{
				return;
			}
			grid.addFilter(e -> e.anyFieldContains(searched, msg));
		});
		return searchText;
	}
}
