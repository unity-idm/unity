/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ColumnToggleMenu extends ContextMenu
{
	private final Map<Grid.Column<?>, MenuItem> columns = new HashMap<>();
	private final Runnable clickListener;

	public ColumnToggleMenu()
	{
		super(VaadinIcon.ELLIPSIS_DOTS_V.create());
		setOpenOnClick(true);
		clickListener = null;
	}

	public ColumnToggleMenu(Runnable clickListener)
	{
		super(VaadinIcon.ELLIPSIS_DOTS_V.create());
		this.clickListener = clickListener;
		setOpenOnClick(true);
	}

	public void addColumn(String label, Grid.Column<?> column)
	{
		MenuItem menuItem = this.addItem(label, event ->
		{
			column.setVisible(event.getSource().isChecked());
			Optional.ofNullable(clickListener).ifPresent(Runnable::run);
		});
		//prevent close on click
		menuItem.getElement().addEventListener("click", event ->
		{
			if (menuItem.isChecked())
				menuItem.getElement().executeJs("this.setAttribute('menu-item-checked', '')");
			else
				menuItem.getElement().executeJs("this.removeAttribute('menu-item-checked')");
		}).addEventData("event.preventDefault()");
		menuItem.setCheckable(true);
		menuItem.setChecked(column.isVisible());
		columns.put(column, menuItem);
	}

	public void setChecked(Grid.Column<?> column, boolean checked)
	{
		Optional.ofNullable(columns.get(column)).ifPresent(menu -> menu.setChecked(checked));
	}

}
