/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.layout;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewProvider;
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
import io.imunity.webelements.navigation.AppContextViewProvider;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Main layout of left sidebar like endpoint
 * 
 * @author P.Piernik
 *
 */
public class SidebarLayout extends CustomComponent
{

	private NavigationHierarchyManager navMan;
	private ViewProvider viewProvider;

	private HorizontalLayout rootContent;
	private BreadCrumbs breadcrumbs;
	private Layout naviContent;
	private TopRightMenu topRightMenu;
	private LeftMenu leftMenu;
	private Component topComponent;
	private UnityView errorView;

	public static SidebarLayout get(NavigationHierarchyManager viewMan)
	{
		return new SidebarLayout(viewMan);
	}

	public SidebarLayout(NavigationHierarchyManager viewMan)
	{
		this.navMan = viewMan;
		setSizeFull();
		setStyleName(Styles.sidebar.toString());

		topRightMenu = new TopRightMenu();
		leftMenu = new LeftMenu(viewMan);
	}
	
	private Component getDefaultTopComponent()
	{
		ViewHeader viewHedear = new ViewHeader();
		UI.getCurrent().getNavigator().addViewChangeListener(viewHedear);
		return viewHedear;
	}

	public SidebarLayout build()
	{
		UI ui = UI.getCurrent();

		if (naviContent == null)
		{
			naviContent = new VerticalLayout();
		}
		naviContent.setSizeFull();
		naviContent.setStyleName(Styles.contentBox.toString());

		new Navigator(ui, naviContent);

		ui.getNavigator().addViewChangeListener(leftMenu);

		if (errorView != null)
		{
			ui.getNavigator().setErrorView(errorView);

		}

		if (viewProvider != null)
		{
			ui.getNavigator().addProvider(viewProvider);
		} else
		{
			ui.getNavigator().addProvider(new AppContextViewProvider(navMan));
		}

		rootContent = new HorizontalLayout();
		rootContent.setSizeFull();
		rootContent.setMargin(false);
		rootContent.setSpacing(false);
		rootContent.setStyleName(Styles.rootContent.toString());


		HorizontalLayout headerBar = new HorizontalLayout();
		headerBar.setSpacing(false);
		headerBar.setMargin(new MarginInfo(false, true));
		headerBar.setStyleName(Styles.headerBar.toString());
		headerBar.setWidth(100, Unit.PERCENTAGE);
		headerBar.setHeightUndefined();
		
		if (topComponent == null)
		{
			topComponent = getDefaultTopComponent();
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

	public BreadCrumbs getBreadCrumbs()
	{
		return breadcrumbs;
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

	public SidebarLayout withViewProvider(ViewProvider provider)
	{
		this.viewProvider = provider;
		return this;
	}

	public SidebarLayout withErrorView(UnityView errorView)
	{
		this.errorView = errorView;
		return this;
	}

	public SidebarLayout withTopComponent(Component topComponent)
	{
		this.topComponent = topComponent;
		return this;
	}
}