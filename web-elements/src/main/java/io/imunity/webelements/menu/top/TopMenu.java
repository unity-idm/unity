/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webelements.menu.top;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.ui.HorizontalLayout;

import io.imunity.webelements.menu.MenuElement;
import io.imunity.webelements.menu.MenuElementContainer;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Top menu bar
 * 
 * @author P.Piernik
 *
 */
public class TopMenu extends HorizontalLayout implements MenuElementContainer
{
	private Map<String, MenuElement> menuElements;
	
	public TopMenu()
	{
		setHeight(0, Unit.PIXELS);
		setStyleName(Styles.topMenu.toString());
		setMargin(false);
		setSpacing(false);
		menuElements = new HashMap<>();
	}

	@Override
	public void addMenuElement(MenuElement entry)
	{
		if (entry.getCaption() != null && !entry.getCaption().trim().isEmpty()) {
			entry.setCaption(null);
		}
		menuElements.put(entry.getId(), entry);
		addComponent(entry);		
	}

	@Override
	public void activate()
	{
		setVisible(true);
		
	}

	@Override
	public void deactivate()
	{
		setVisible(false);
		
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