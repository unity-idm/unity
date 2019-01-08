/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.contextmenu.GridContextMenu;
import com.vaadin.ui.Grid;

/**
 * Supports installing {@link ContextMenu} on {@link Grid} using {@link SingleActionHandler}.
 * 
 * @author K. Benedyczak
 */
public class GridContextMenuSupport<T>
{
	private List<SingleActionHandler<T>> actionHandlers = new ArrayList<>();
	
	public GridContextMenuSupport(Grid<T> grid)
	{
		GridContextMenu<T> contextMenu = new GridContextMenu<>(grid);
		contextMenu.addGridBodyContextMenuListener(e ->
		{
			Set<T> selection = new HashSet<>();
			if (e.getItem() != null)
				selection.add((T) e.getItem());
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
		contextMenu.removeItems();
		for (SingleActionHandler<T> handler: actionHandlers)
		{
			if (handler.isVisible(selection))
			{
				contextMenu.addItem(handler.getCaption(), 
						handler.getIcon(), 
						(mi) -> handler.handle(selection))
					.setEnabled(handler.isEnabled(selection));
			}
		}
	}
}
