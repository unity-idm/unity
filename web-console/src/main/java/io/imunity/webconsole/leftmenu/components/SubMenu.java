/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.leftmenu.components;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.layout.MenuComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Submenu component. Contains show/hide button and layout for submenu content. 
 * @author P.Piernik
 *
 */
public class SubMenu extends VerticalLayout implements MenuComponent<VerticalLayout>
{
	private MenuButton button;
	private VerticalLayout content;
	
	public static SubMenu get()
	{
		return new SubMenu("");
	}

	public SubMenu(String caption)
	{
		build(caption, null);
	}

	public SubMenu(Resource icon)
	{
		build(null, icon);
	}

	public SubMenu(String caption, Resource icon)
	{
		build(caption, icon);
	}

	private void build(String caption, Resource icon)
	{
		button = MenuButton.get().withCaption(caption).withIcon(icon)
				.withToolTip(Images.bottomArrow.getHtml())
				.withClickListener(e -> toggle());
		
		content = new VerticalLayout();
		content.setMargin(false);
		content.setSpacing(false);
		setMargin(false);
		setSpacing(false);
		addComponents(button, content);
		setPrimaryStyleName(Styles.subMenu.toString());
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
		button.setActive(true);
		addStyleName(Styles.subMenuOpen.toString());
		return this;
	}

	public SubMenu close()
	{
		button.setActive(false);
		removeStyleName(Styles.subMenuOpen.toString());
		return this;
	}

	public boolean isOpen()
	{
		return getStyleName().contains(Styles.subMenuOpen.toString());
	}

	@Override
	public String getRootStyle()
	{
		return Styles.subMenu.toString();
	}

	public <C extends MenuComponent<?>> C add(C c)
	{
		content.addComponent(c);
		return c;
	}

	@Override
	public <C extends MenuComponent<?>> C addAsFirst(C c)
	{
		content.addComponentAsFirst(c);
		return c;
	}

	@Override
	public <C extends MenuComponent<?>> C addAt(C c, int index)
	{
		content.addComponent(c, index);
		return c;
	}

	@Override
	public <C extends MenuComponent<?>> SubMenu remove(C c)
	{
		content.removeComponent(c);
		return this;
	}

	@Override
	public int count()
	{
		return getList().size();
	}

	public List<MenuComponent<?>> getList()
	{
		List<MenuComponent<?>> menuComponentList = new ArrayList<MenuComponent<?>>();
		for (int i = 0; i < content.getComponentCount(); i++)
		{
			Component component = content.getComponent(i);
			if (component instanceof MenuComponent<?>)
			{
				menuComponentList.add((MenuComponent<?>) component);
			}
		}
		return menuComponentList;
	}
}