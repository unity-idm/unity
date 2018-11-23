/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.layout;

import com.vaadin.shared.ui.MarginInfo;
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
import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Main layout of left sidebar like endpoint
 * 
 * @author P.Piernik
 *
 */
public class SidebarLayout extends CustomComponent
{
	private HorizontalLayout rootContent;
	private TopRightMenu topRightMenu;
	private LeftMenu leftMenu;

	public SidebarLayout(NavigationHierarchyManager viewMan, Layout naviContent,
			Component topComponent)
	{
		setSizeFull();
		setStyleName(SidebarStyles.sidebar.toString());
		this.topRightMenu = new TopRightMenu();
		this.leftMenu = new LeftMenu(viewMan);

		UI ui = UI.getCurrent();
		if (ui.getNavigator() != null)
			ui.getNavigator().addViewChangeListener(leftMenu);

		rootContent = new HorizontalLayout();
		rootContent.setSizeFull();
		rootContent.setMargin(false);
		rootContent.setSpacing(false);
		rootContent.setStyleName(SidebarStyles.rootContent.toString());

		HorizontalLayout headerBar = new HorizontalLayout();
		headerBar.setSpacing(false);
		headerBar.setMargin(new MarginInfo(false, true));
		headerBar.setStyleName(SidebarStyles.headerBar.toString());
		headerBar.setWidth(100, Unit.PERCENTAGE);
		headerBar.setHeightUndefined();

		if (topComponent == null)
		{
			throw new IllegalArgumentException("Top component cannot be null");
		}

		if (naviContent == null)
		{
			throw new IllegalArgumentException(
					"Navigation content component cannot be null");
		}

		headerBar.addComponent(topComponent);
		headerBar.setComponentAlignment(topComponent, Alignment.MIDDLE_LEFT);
		headerBar.addComponent(topRightMenu);
		headerBar.setComponentAlignment(topRightMenu, Alignment.MIDDLE_RIGHT);

		VerticalLayout rightSpace = new VerticalLayout();
		rightSpace.setMargin(false);
		rightSpace.setSpacing(false);
		rightSpace.setSizeFull();
		rightSpace.addComponents(headerBar, naviContent);
		rightSpace.setExpandRatio(naviContent, 1f);

		rootContent.addComponents(leftMenu, rightSpace);
		rootContent.setExpandRatio(rightSpace, 1f);

		setCompositionRoot(rootContent);
	}

	public LeftMenu getLeftMenu()
	{
		return leftMenu;
	}

	public TopRightMenu getTopRightMenu()
	{
		return topRightMenu;
	}
}