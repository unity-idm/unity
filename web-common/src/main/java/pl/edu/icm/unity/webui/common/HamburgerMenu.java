/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.server.Resource;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.themes.ValoTheme;

/**
 * MenuBar with one main hamburger button to show/hide menu
 * @author P.Piernik
 *
 */
public class HamburgerMenu extends MenuBar
{
	private MenuItem top;
	
	public HamburgerMenu()
	{
		top = super.addItem("", Images.vaadinMenu.getResource(), null);
		top.setStyleName(Styles.hamburgerMenu.toString());
		setStyleName(ValoTheme.MENUBAR_BORDERLESS);
	}
	
	@Override
	public MenuItem addItem(String caption, Resource icon, Command command)
	{
		MenuItem item = top.addItem(caption, command);
		item.setIcon(icon);
		return item;
	}
}
