/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.menu.left;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.menu.MenuElement;
import io.imunity.webelements.menu.MenuElementContainer;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Toggleable left menu
 * 
 * @author P.Piernik
 *
 */
public class LeftMenu extends CustomComponent implements ViewChangeListener, MenuElementContainer
{

	public enum ToggleMode
	{
		NORMAL, MINIMAL;
	}

	private NavigationHierarchyManager navMan;
	private VerticalLayout main;
	private Map<String, MenuElement> menuElements;
	private Map<String, MenuElementContainer> menuContainers;
	private Map<String, MenuElement> allElements;
	private List<MenuElement> activatedElements;
	private ToggleMode toggleMode;
	private Button toggleButton;

	public LeftMenu(NavigationHierarchyManager navMan)
	{
		this.navMan = navMan;
		main = new VerticalLayout();
		setWidth(250, Unit.PIXELS);
		setHeight(100, Unit.PERCENTAGE);
		setStyleName(SidebarStyles.leftMenu.toString());
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);
		menuElements = new HashMap<>();
		menuContainers = new HashMap<>();
		allElements = new HashMap<>();
		activatedElements = new ArrayList<>();

		toggleMode = ToggleMode.NORMAL;
		toggleButton = new Button();
		toggleButton.setIcon(Images.leftDoubleArrow.getResource());
		main.addComponent(toggleButton);
		toggleButton.addClickListener(e -> toggleSize());
		toggleButton.addStyleName(Styles.vButtonBorderless.toString());
		//toggleButton.addStyleName(Styles.menuButton.toString());
		toggleButton.addStyleName("u-left-menu-toggle");
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
				toggleButton.setIcon(Images.rightDoubleArrow.getResource());
				getParent().addStyleName(ToggleMode.MINIMAL.name().toLowerCase());
				break;
			case NORMAL:
				setWidth(250, Unit.EM);
				toggleButton.setIcon(Images.leftDoubleArrow.getResource());
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

	public void addSubContainerElement(MenuElementContainer container)
	{
		menuContainers.put(container.getMenuElementId(), container);
		allElements.put(container.getMenuElementId(), container);

		for (MenuElement element : container.getMenuElements())
		{
			allElements.put(element.getMenuElementId(), element);
		}

		main.addComponent(container);
	}

	private void adapt(List<NavigationInfo> path)
	{
		for (MenuElement activeted : activatedElements)
		{
			activeted.setActive(false);
		}
		activatedElements.clear();

		for (MenuElementContainer container : menuContainers.values())
		{
			container.setActive(false);
		}

		for (NavigationInfo view : path)
		{
			if (allElements.containsKey(view.id))
			{
				MenuElement toActive = allElements.get(view.id);
				toActive.setActive(true);
				activatedElements.add(toActive);
			}
		}
	}

	@Override
	public String getMenuElementId()
	{
		return super.getId();
	}

	@Override
	public void addMenuElement(MenuElement entry)
	{
		menuElements.put(entry.getMenuElementId(), entry);
		allElements.put(entry.getMenuElementId(), entry);
		main.addComponent(entry);
	}

	@Override
	public Collection<MenuElement> getMenuElements()
	{
		return menuElements.values();
	}

	public Collection<MenuElementContainer> getSubContainerElements()
	{
		return menuContainers.values();
	}

	public void addNavigationElements(String rootNavElement)

	{
		for (NavigationInfo child : navMan.getChildren(rootNavElement))
		{
			if (child.type == Type.ViewGroup)
			{

				MenuElementContainer subMenu = SubMenu.get(child.id).withCaption(getCaption(child))
						.withIcon(child.icon);
				buildSubMenu(navMan.getChildren(child.id), subMenu);
				addSubContainerElement(subMenu);

			} else if (child.type == Type.View || child.type == Type.DefaultView)
			{
				addMenuElement(MenuButton.get(child.id).withCaption(getCaption(child))
						.withNavigateTo(child.id).withIcon(child.icon));
			}
		}
	}

	private void buildSubMenu(List<NavigationInfo> viewChildren, MenuElementContainer menuContainer)
	{
		for (NavigationInfo child : viewChildren)
		{
			menuContainer.addMenuElement(MenuButton.get(child.id).withCaption(getCaption(child))
					.withNavigateTo(child.id).withIcon(child.icon));
		}
	}

	String getCaption(NavigationInfo element)
	{
		return element.shortCaption != null ? element.shortCaption : element.caption;
	}
	
	@Override
	public boolean beforeViewChange(ViewChangeEvent event)
	{
		return true;
	}

	@Override
	public void afterViewChange(ViewChangeEvent event)
	{
		adapt(navMan.getParentPath(event.getViewName()));
	}

	@Override
	public void setActive(boolean active)
	{
		setVisible(active);
	}
	
	public void setToggleVisible(boolean visible)
	{
		toggleButton.setVisible(visible);
	}
}