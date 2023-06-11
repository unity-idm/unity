/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.grid;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.SearchField;

/**
 * Helps create search field for {@link FilterableGrid}
 * 
 * @author P.Piernik
 *
 */
public class FilterableGridHelper
{
	public static SearchField generateSearchField(FilterableGrid<? extends FilterableEntry> grid,
			MessageSource msg)
	{
		SearchField searchText = getRowSearchField(msg);
		searchText.addValueChangeListener(event -> {
			String searched = event.getValue();
			grid.clearFilters();
			if (event.getValue() == null || event.getValue().isEmpty())
			{
				return;
			}
			grid.addFilter(e -> e.anyFieldContains(searched, msg));
		});
		return searchText;
	}
	
	public static SearchField getRowSearchField(MessageSource msg)
	{
		return new SearchField(msg);
	}
}
