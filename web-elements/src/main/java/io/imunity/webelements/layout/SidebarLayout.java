/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.layout;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.menu.left.LeftMenu;
import io.imunity.webelements.menu.top.TopRightMenu;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Main layout of left sidebar like endpoint
 * 
 * @author P.Piernik
 *
 */
public class SidebarLayout extends CustomComponent
{
	private HorizontalLayout rootContent;
	private Layout naviContent;
	private TopRightMenu topRightMenu;
	private LeftMenu leftMenu;
	private Component topComponent;

	public static SidebarLayout get(NavigationHierarchyManager viewMan)
	{
		return new SidebarLayout(viewMan);
	}

	public SidebarLayout(NavigationHierarchyManager viewMan)
	{
		setSizeFull();
		setStyleName(Styles.sidebar.toString());

		topRightMenu = new TopRightMenu();
		leftMenu = new LeftMenu(viewMan);
	}

	public SidebarLayout build()
	{

		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);

		UI ui = UI.getCurrent();
		if (ui.getNavigator() != null)
			ui.getNavigator().addViewChangeListener(leftMenu);

		rootContent = new HorizontalLayout();
		rootContent.setSizeFull();
		rootContent.setMargin(false);
		rootContent.setSpacing(false);
		rootContent.setStyleName(Styles.rootContent.toString());

		VerticalLayout naviContentWrapper = new VerticalLayout();
		naviContentWrapper.setMargin(false);
		naviContentWrapper.setSpacing(false);
		naviContentWrapper.setSizeFull();

		HorizontalLayout headerBar = new HorizontalLayout();
		headerBar.setSpacing(false);
		headerBar.setMargin(false);
		headerBar.setStyleName(Styles.headerBar.toString());
		headerBar.setWidth(100, Unit.PERCENTAGE);
		if (topComponent == null)
		{
			topComponent = new HorizontalLayout();
		}
		
		if (naviContent == null)
		{
			naviContent = new VerticalLayout();
			naviContent.setStyleName(Styles.contentBox.toString());
		}

		headerBar.addComponent(topComponent);
		headerBar.setComponentAlignment(topComponent, Alignment.MIDDLE_LEFT);
		headerBar.setExpandRatio(topComponent, 1);

		headerBar.addComponent(topRightMenu);
		headerBar.setComponentAlignment(topRightMenu, Alignment.MIDDLE_RIGHT);

		naviContentWrapper.addComponents(headerBar, naviContent);
		naviContentWrapper.setExpandRatio(naviContent, 1f);

		rootContent.addComponents(leftMenu, naviContentWrapper);
		rootContent.setExpandRatio(naviContentWrapper, 1f);

		main.addComponent(rootContent);
		main.setExpandRatio(rootContent, 1f);

		addStyleName(Styles.sidebarWhite.toString());
		setCompositionRoot(main);

		return this;
	}

	public LeftMenu getLeftMenu()
	{
		return leftMenu;
	}

	public TopRightMenu getTopRightMenu()
	{
		return topRightMenu;
	}

	public Layout getNaviContent()
	{
		return naviContent;
	}

	public SidebarLayout withNaviContent(Layout naviRootContent)
	{
		this.naviContent = naviRootContent;
		return this;
	}

	public SidebarLayout withTopComponent(Component topComponent)
	{
		this.topComponent = topComponent;
		return this;
	}
}