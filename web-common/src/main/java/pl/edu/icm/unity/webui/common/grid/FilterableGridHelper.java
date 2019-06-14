/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.grid;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Helps create search field for {@link FilterableGrid}
 * 
 * @author P.Piernik
 *
 */
public class FilterableGridHelper
{
	public static TextField generateSearchField(FilterableGrid<? extends FilterableEntry> grid,
			UnityMessageSource msg)
	{
		TextField searchText = getRowSearchField(msg);
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
	
	public static TextField getRowSearchField(UnityMessageSource msg)
	{
		TextField searchText = new TextField();
		searchText.addStyleName(Styles.vSmall.toString());
		searchText.setWidth(10, Unit.EM);
		searchText.setPlaceholder(msg.getMessage("search"));
		return searchText;
	}
}
