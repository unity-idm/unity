/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.grid.Grid;

public class MultiSelectGrid<T> extends Grid<T>
{
	public MultiSelectGrid()
	{
		setThemeName("no-border");
		setSelectionMode(Grid.SelectionMode.MULTI);
		setColumnReorderingAllowed(true);
		addItemClickListener(event ->
		{
			if(getSelectedItems().contains(event.getItem()))
				deselect(event.getItem());
			else
				select(event.getItem());
		});
	}
}
