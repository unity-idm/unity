/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.grid;

import io.imunity.vaadin.elements.SearchField;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Helps create search field for {@link FilterableGrid}
 * 
 * @author P.Piernik
 *
 */
public class GridSearchFieldFactory
{
	public static SearchField generateSearchField(FilterableGrid<? extends FilterableEntry> grid, MessageSource msg)
	{
		SearchField searchText = new SearchField(msg.getMessage("search"), searched ->
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
