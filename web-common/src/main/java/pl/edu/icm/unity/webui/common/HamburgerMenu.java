/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vaadin.event.selection.SelectionListener;
import com.vaadin.server.Resource;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.themes.ValoTheme;

/**
 * MenuBar with one main hamburger button to show/hide menu
 * @author P.Piernik
 *
 */
public class HamburgerMenu<T> extends MenuBar
{
	private MenuItem top;
	private Set<T> target;
	private Map<MenuItem, SingleActionHandler<T>> items;

	public HamburgerMenu()
	{
		items = new HashMap<>();
		target = Collections.emptySet();
		top = super.addItem("", Images.menu.getResource(), null);
		top.setStyleName(Styles.hamburgerMenu.toString());
		setStyleName(ValoTheme.MENUBAR_BORDERLESS);
	}

	public void addSeparator()
	{
		top.addSeparator();
	}

	@Override
	public MenuItem addItem(String caption, Resource icon, Command command)
	{
		MenuItem item = top.addItem(caption, command);
		item.setIcon(icon);
		return item;
	}

	public SelectionListener<T> getSelectionListener()
	{
		return event -> {
			target = event.getAllSelectedItems();
			for (MenuItem item : items.keySet())
				updateMenuState(item);
		};
	}

	public void addActionHandlers(Collection<SingleActionHandler<T>> handlers)
	{
		for (SingleActionHandler<T> handler : handlers)
			addActionHandler(handler);
	}

	public void addActionHandler(SingleActionHandler<T> handler)
	{
		MenuItem menuItem = addItem(handler.getCaption(), c -> {
			if (!handler.isEnabled(target))
				return;
			handler.handle(target);
		});
		menuItem.setIcon(handler.getIcon());
		items.put(menuItem, handler);
		updateMenuState(menuItem);
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
}
