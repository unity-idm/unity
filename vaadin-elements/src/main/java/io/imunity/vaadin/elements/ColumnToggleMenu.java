/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;

public class ColumnToggleMenu extends ContextMenu
{
	public ColumnToggleMenu()
	{
		super(VaadinIcon.ELLIPSIS_DOTS_V.create());
		setOpenOnClick(true);
	}

	public void addColumn(String label, Grid.Column<?> column)
	{
		MenuItem menuItem = this.addItem(label, event -> column.setVisible(event.getSource().isChecked()));
		menuItem.setCheckable(true);
		menuItem.setChecked(column.isVisible());
	}

}
