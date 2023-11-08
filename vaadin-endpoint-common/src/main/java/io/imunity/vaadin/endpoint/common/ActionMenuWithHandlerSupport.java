package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.selection.SelectionListener;
import io.imunity.vaadin.elements.ActionMenu;
import io.imunity.vaadin.elements.MenuButton;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;

import java.util.*;

public class ActionMenuWithHandlerSupport<T> extends ActionMenu
{
	private Set<T> target;
	private Map<MenuItem, SingleActionHandler<T>> items;

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

	private Component createIcon(Icon vaadinIcon)
	{
		vaadinIcon.getStyle()
				.set("margin-inline-end", "var(--lumo-space-s")
				.set("padding", "var(--lumo-space-xs");
		return vaadinIcon;
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
