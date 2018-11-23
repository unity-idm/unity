/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webelements.menu.top;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

import io.imunity.webelements.menu.MenuElement;
import io.imunity.webelements.menu.MenuElementContainer;
import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Top right menu bar
 * 
 * @author P.Piernik
 *
 */
public class TopRightMenu extends CustomComponent implements MenuElementContainer
{
	private Map<String, MenuElement> menuElements;
	private HorizontalLayout layout;
	
	public TopRightMenu()
	{
		layout = new HorizontalLayout();
		layout.setWidthUndefined();
		setStyleName(SidebarStyles.topRightMenu.toString());
		layout.setMargin(false);
		layout.setSpacing(false);
		menuElements = new HashMap<>();
		setCompositionRoot(layout);
		setWidthUndefined();
	}

	@Override
	public void addMenuElement(MenuElement entry)
	{
		menuElements.put(entry.getId(), entry);
		layout.addComponent(entry);
		layout.setComponentAlignment(entry, Alignment.MIDDLE_CENTER);
	}

	@Override
	public void setActive(boolean active)
	{
		setVisible(active);
		
	}

	@Override
	public Collection<MenuElement> getMenuElements()
	{
		return menuElements.values();
	}

	@Override
	public String getMenuElementId()
	{
		return super.getId();
	}
}