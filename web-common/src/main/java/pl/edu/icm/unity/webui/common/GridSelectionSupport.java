/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Grid;

/**
 * Adds simplified selection support for multiselect grids. We allow for selecting a row 
 * by clicking on it.
 * 
 * @author K. Benedyczak
 */
public class GridSelectionSupport
{
	public static <T> void installClickListener(Grid<T> grid)
	{
		grid.addItemClickListener(e -> onMouseClick(grid, e));
	}
	
	private static <T> void onMouseClick(Grid<T> grid, Grid.ItemClick<T> event)
	{
		if (event.getMouseEventDetails().isDoubleClick())
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
