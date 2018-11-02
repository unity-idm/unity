/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.leftMenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.common.MenuElement;
import io.imunity.webelements.common.MenuElementContainer;
import io.imunity.webelements.navigation.NavigationInfo;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Toggleable left menu
 * 
 * @author P.Piernik
 *
 */
public class LeftMenu extends CustomComponent
{

	public enum ToggleMode
	{
		NORMAL, MINIMAL;
	}

	private VerticalLayout main;
	private Map<String, MenuElement> menuElements;
	private Map<String, MenuElementContainer> menuContainers;
	private Map<String, MenuElement> allElements;
	private List<MenuElement> activatedElements;
	private ToggleMode toggleMode;

	public LeftMenu()
	{

		main = new VerticalLayout();
		setWidth(250, Unit.EM);
		setHeight(100, Unit.PERCENTAGE);
		setStyleName(Styles.leftMenu.toString());
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);
		menuElements = new HashMap<>();
		menuContainers = new HashMap<>();
		allElements = new HashMap<>();
		activatedElements = new ArrayList<>();
		
		toggleMode = ToggleMode.NORMAL;
	}

	public LeftMenu toggleSize()
	{
		if (getToggleMode().equals(ToggleMode.NORMAL))
		{
			setToggleMode(ToggleMode.MINIMAL);
		} else
		{
			setToggleMode(ToggleMode.NORMAL);
		}
		return this;
	}

	public LeftMenu setToggleMode(ToggleMode toggleMode)
	{
		if (toggleMode != null)
		{
			switch (toggleMode)
			{
			case MINIMAL:
				setWidth(50, Unit.EM);
				getParent().addStyleName(ToggleMode.MINIMAL.name().toLowerCase());
				break;
			case NORMAL:
				setWidth(250, Unit.EM);
				getParent().removeStyleName(
						ToggleMode.MINIMAL.name().toLowerCase());
				break;
			}
			this.toggleMode = toggleMode;
		}
		return this;
	}

	public ToggleMode getToggleMode()
	{
		return toggleMode;
	}

	public void activate()
	{
		setVisible(true);

	}

	public void deactivate()
	{
		setVisible(false);

	}

	public void addEntry(MenuElement entry)
	{

		menuElements.put(entry.getMenuElementId(), entry);
		allElements.put(entry.getMenuElementId(), entry);
		main.addComponent(entry);
	}

	public void addSubContainerEntry(MenuElementContainer container)
	{
		menuContainers.put(container.getMenuElementId(), container);
		allElements.put(container.getMenuElementId(), container);

		for (MenuElement element : container.getMenuElements())
		{
			allElements.put(element.getMenuElementId(), element);
		}

		main.addComponent(container);
	}

	public void adapt(List<NavigationInfo> path)
	{
		for (MenuElement activeted : activatedElements)
		{
			activeted.deactivate();
		}
		activatedElements.clear();

		for (MenuElementContainer container : menuContainers.values())
		{
			container.deactivate();
		}
		
		for (NavigationInfo view : path)
		{
			if (allElements.containsKey(view.id))
			{
				MenuElement toActive = allElements.get(view.id);
				toActive.activate();
				activatedElements.add(toActive);
			}
		}
	}

	public Collection<MenuElement> getEntries()
	{
		return menuElements.values();
	}

	public Collection<MenuElementContainer> getSubContainerEntries()
	{
		return menuContainers.values();
	}
}