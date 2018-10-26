/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.layout;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;

import io.imunity.webconsole.leftmenu.components.MenuButton;
import io.imunity.webconsole.leftmenu.components.MenuLabel;
import io.imunity.webconsole.leftmenu.components.SubMenu;

/**
 * Controls menu and breadcrumbs refreshing after each view change
 * 
 * @author P.Piernik
 *
 */
public class DefaultViewChangeManager
{

	public List<MenuComponent<?>> init(WebConsoleLayout hybridMenu)
	{
		hybridMenu.getBreadCrumbs().clear();
		return new ArrayList<MenuComponent<?>>();
	}

	public boolean manage(WebConsoleLayout wbLayout, MenuComponent<?> menuComponent,
			ViewChangeEvent event, List<MenuComponent<?>> menuContentList)
	{
		boolean foundActiveButton = false;
		if (menuComponent != null)
		{
			List<MenuComponent<?>> cacheMenuContentList = new ArrayList<MenuComponent<?>>();

			if (menuComponent.getList() != null)
			{
				for (MenuComponent<?> cacheMenuComponent : menuComponent.getList())
				{
					if (manage(wbLayout, cacheMenuComponent, event,
							cacheMenuContentList))
					{
						foundActiveButton = true;
					}
				}
			}
			if (menuComponent instanceof MenuButton)
			{
				MenuButton current = (MenuButton) menuComponent;
				current.setActive(foundActiveButton);
				if (foundActiveButton)
				{
					add(wbLayout, MenuButton.get()
							.withCaption(current.getBreadCrumbProvider()
									.getBreadCrumb(event))
							.clickable().withClickListener(
									e -> ((MenuButton) menuComponent)
											.click()),
							menuContentList);

				} else

				if (checkButton(current, event))
				{
					add(wbLayout, MenuButton.get()
							.withCaption(current.getBreadCrumbProvider()
									.getBreadCrumb(event)),
							cacheMenuContentList);
					foundActiveButton = true;

				}

			} else if (menuComponent instanceof LeftMenu
					|| menuComponent instanceof SubMenu)
			{
				if (menuComponent instanceof SubMenu)
				{
					SubMenu hmSubMenu = (SubMenu) menuComponent;
					if (foundActiveButton)
					{
						MenuButton breadCrumButton = MenuButton.get()
								.withCaption(hmSubMenu
										.getCaption());
						breadCrumButton.setToolTip(
								hmSubMenu.getButton().getToolTip());
						breadCrumButton.removeToolTip();
						add(wbLayout, breadCrumButton, menuContentList);
						hmSubMenu.open();
					} else
					{
						hmSubMenu.close();
					}
				}

			}

			menuContentList.addAll(cacheMenuContentList);
		}

		return foundActiveButton;
	}

	public void add(WebConsoleLayout wbLayout, MenuComponent<?> menuComponent,
			List<MenuComponent<?>> menuContentList)
	{
		if (wbLayout.getBreadCrumbs().getRoot() != null || menuContentList.size() > 0)
		{
			menuContentList.add(MenuLabel.get()
					.withCaption(BreadCrumbs.BREADCRUMB_SEPARATOR));
		}
		menuContentList.add(menuComponent);
	}

	public boolean checkButton(MenuButton button, ViewChangeEvent event)
	{
		boolean check = false;

		if (button.getNavigateTo() != null)
		{
			if (button.getNavigateTo()
					.startsWith(event.getNewView().getClass().getSimpleName()))
			{
				if (button.getNavigateTo().equals(event.getViewName()))
				{
					check = true;
				} else if (button.getNavigateTo().equals(
						event.getViewName() + "/" + event.getParameters()))
				{
					check = true;
				}
			} else
			{
				if (button.getNavigateTo().equals(event.getViewName()))
				{
					check = true;
				}
			}
		}

		button.setActive(check);

		return check;
	}

	public void finish(WebConsoleLayout hybridMenu, List<MenuComponent<?>> menuContentList)
	{
		if (hybridMenu.getBreadCrumbs().getRoot() != null && menuContentList.size() == 2)
		{
			MenuComponent<?> menuContent = menuContentList.get(1);
			if (hybridMenu.getBreadCrumbs().getRoot().getCaption()
					.equals(menuContent.getCaption()))
			{
				menuContentList.clear();
			}
		}

		for (MenuComponent<?> menuComponent : menuContentList)
		{
			hybridMenu.getBreadCrumbs().add(menuComponent);
		}
	}
}