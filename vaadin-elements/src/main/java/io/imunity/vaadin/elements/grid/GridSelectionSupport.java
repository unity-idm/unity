/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements.grid;


import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;

public class GridSelectionSupport
{
	public static <T> void installClickListener(Grid<T> grid)
	{
		grid.addItemClickListener(e -> onMouseClick(grid, e));
	}

	public static <T> void replaceSelection(Grid<T> grid, T item)
	{
		if (grid.getSelectedItems().contains(item))
			return;
		grid.deselectAll();
		grid.select(item);
	}

	private static <T> void onMouseClick(Grid<T> grid, ItemClickEvent<T> event)
	{
		if (event.getClickCount() == 2)
			return;
		replaceSelection(grid, event.getItem());
	}
}
