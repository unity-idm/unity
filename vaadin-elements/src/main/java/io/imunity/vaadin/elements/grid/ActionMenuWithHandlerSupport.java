package io.imunity.vaadin.elements.grid;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.selection.SelectionListener;

import io.imunity.vaadin.elements.ActionMenu;
import io.imunity.vaadin.elements.MenuButton;

public class ActionMenuWithHandlerSupport<T> extends ActionMenu
{
	private Set<T> target;
	private final Map<MenuItem, SingleActionHandler<T>> items;

	public ActionMenuWithHandlerSupport()
	{
		items = new HashMap<>();
		this.target = Collections.emptySet();
	}

	public MenuItem addActionHandler(SingleActionHandler<T> handler)
	{
		MenuItem menuItem = addItem(new MenuButton(handler.getCaption(), handler.getIcon()), c ->
		{
			if (!handler.isEnabled(target))
				return;
			handler.handle(target);
		});
		items.put(menuItem, handler);
		updateMenuState(menuItem);
		return menuItem;
	}

	public void addActionHandlers(Collection<SingleActionHandler<T>> handlers)
	{
		for (SingleActionHandler<T> handler : handlers)
			addActionHandler(handler);
	}

	private void updateMenuState(MenuItem item)
	{
		SingleActionHandler<T> handler = items.get(item);

		if (handler.isVisible(target))
		{
			item.setVisible(true);
			item.setEnabled(handler.isEnabled(target));
		} else
		{
			item.setVisible(false);
		}
	}

	public void setTarget(Set<T> target)
	{
		this.target = target;
	}

	public SelectionListener<Grid<T>, T> getSelectionListener()
	{
		return event ->
		{
			target = event.getAllSelectedItems();
			for (MenuItem item : items.keySet())
				updateMenuState(item);
		};
	}
}
