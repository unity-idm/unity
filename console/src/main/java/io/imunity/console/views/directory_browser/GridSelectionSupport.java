/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser;


import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;

public class GridSelectionSupport
{
	public static <T> void installClickListener(Grid<T> grid)
	{
		grid.addItemClickListener(e -> onMouseClick(grid, e));
	}
	
	private static <T> void onMouseClick(Grid<T> grid, ItemClickEvent<T> event)
	{
		if (event.getClickCount() == 2)
			return;
		T item = event.getItem();
		boolean alreadySelected = grid.getSelectedItems().contains(item);
		if (!alreadySelected)
		{
			grid.deselectAll();
			grid.select(item);
		}
	}
}
