/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.menu;

import com.vaadin.server.Resource;
import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Left menu combobox
 * 
 * @author P.Piernik
 * @param <T>
 *
 */
public class MenuComoboBox extends ComboBox<String> implements MenuElement
{
	public static MenuComoboBox get()
	{
		return new MenuComoboBox("");
	}

	public MenuComoboBox(String caption)
	{
		build(caption, null);
	}

	public MenuComoboBox(String caption, Resource icon)
	{
		build(caption, icon);
	}

	private void build(String caption, Resource icon)
	{

		withCaption(caption);
		withIcon(icon);
		setStyleName(SidebarStyles.menuCombo.toString());
		
	}

	public MenuComoboBox withStyleName(String style)
	{
		addStyleName(style);
		return this;
	}

	public MenuComoboBox withCaption(String caption)
	{
		super.setCaption(caption);
		return this;
	}

	public MenuComoboBox withIcon(Resource icon)
	{
		super.setIcon(icon);
		return this;
	}

	@Override
	public void setActive(boolean active)
	{
		setEnabled(active);

	}

	@Override
	public String getMenuElementId()
	{
		return super.getId();
	}
}
