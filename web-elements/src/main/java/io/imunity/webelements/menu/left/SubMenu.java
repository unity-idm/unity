/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.menu.left;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.server.Resource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.menu.MenuElement;
import io.imunity.webelements.menu.MenuElementContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Submenu component. Contains show/hide button and layout for submenu content.
 * 
 * @author P.Piernik
 *
 */
public class SubMenu extends CustomComponent implements MenuElementContainer
{
	private MenuButton button;
	private VerticalLayout content;
	private Map<String, MenuElement> menuElements;
	private String id;
	private VerticalLayout main;

	public static SubMenu get(String id)
	{
		return new SubMenu(id, "");
	}

	public SubMenu(String id, String caption)
	{
		build(id, caption, null);
	}

	public SubMenu(String id, Resource icon)
	{
		build(id, null, icon);
	}

	public SubMenu(String id, String caption, Resource icon)
	{
		build(id, caption, icon);
	}

	private void build(String id, String caption, Resource icon)
	{
		this.id = id;
		main = new VerticalLayout();
		button = MenuButton.get(super.getId()).withCaption(caption).withIcon(icon)
				.withToolTip(Images.bottomArrow.getHtml())
				.withClickListener(e -> toggle());

		content = new VerticalLayout();
		content.setStyleName("menucontent");
		content.setMargin(false);
		content.setSpacing(false);
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponents(button, content);
		setStyleName(SidebarStyles.subMenu.toString());
		menuElements = new HashMap<>();
		setCompositionRoot(main);
	}

	@Override
	public String getCaption()
	{
		return button.getCaption();
	}

	public MenuButton getButton()
	{
		return button;
	}

	public SubMenu withCaption(String caption)
	{
		button.withCaption(caption);
		return this;
	}

	public SubMenu withIcon(Resource icon)
	{
		button.withIcon(icon);
		return this;
	}

	public SubMenu toggle()
	{
		if (isOpen())
		{
			close();
		} else
		{
			open();
		}
		return this;
	}

	public SubMenu open()
	{
		addStyleName(SidebarStyles.subMenuOpen.toString());
		return this;
	}

	public SubMenu close()
	{
		removeStyleName(SidebarStyles.subMenuOpen.toString());
		return this;
	}

	public boolean isOpen()
	{
		return getStyleName().contains(SidebarStyles.subMenuOpen.toString());
	}

	@Override
	public void addMenuElement(MenuElement entry)
	{
		menuElements.put(entry.getMenuElementId(), entry);
		content.addComponent(entry);

	}

	@Override
	public void setActive(boolean active)
	{
		if (active)
		{
			open();
		} else
		{
			close();
		}

	}

	@Override
	public Collection<MenuElement> getMenuElements()
	{
		return menuElements.values();
	}

	@Override
	public String getMenuElementId()
	{
		return id;
	}
}