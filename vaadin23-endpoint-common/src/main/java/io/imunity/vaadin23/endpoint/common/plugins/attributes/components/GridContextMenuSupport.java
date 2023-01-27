/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.plugins.attributes.components;


import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GridContextMenuSupport<T>
{
	private final List<SingleActionHandler<T>> actionHandlers = new ArrayList<>();
	
	public GridContextMenuSupport(Grid<T> grid)
	{
		GridContextMenu<T> contextMenu = new GridContextMenu<>(grid);
		contextMenu.addGridContextMenuOpenedListener(e ->
		{
			Set<T> selection = new HashSet<>();
			if (e.getItem().isPresent())
				selection.add(e.getItem().get());
			fillContextMenu(contextMenu, selection);
		});
	}
	
	public void addActionHandler(SingleActionHandler<T> actionHandler)
	{
		actionHandlers.add(actionHandler);
	}

	public List<SingleActionHandler<T>> getActionHandlers()
	{
		return actionHandlers;
	}
	
	private void fillContextMenu(GridContextMenu<T> contextMenu, Set<T> selection)
	{
		contextMenu.removeAll();
		for (SingleActionHandler<T> handler: actionHandlers)
		{
			if (handler.isVisible(selection))
			{
				contextMenu.addItem(new Div(new Label(handler.getCaption()), handler.getIcon()),
						(mi) -> handler.handle(selection))
					.setEnabled(handler.isEnabled(selection));
			}
		}
	}
}
