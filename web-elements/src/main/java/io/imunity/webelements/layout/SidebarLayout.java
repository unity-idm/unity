/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.layout;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.leftMenu.LeftMenu;
import io.imunity.webelements.navigation.AppContextViewProvider;
import io.imunity.webelements.navigation.NavigationManager;
import io.imunity.webelements.navigation.UnityView;
import io.imunity.webelements.topMenu.TopMenu;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Main layout of left sidebar like endpoint
 * 
 * @author P.Piernik
 *
 */
public class SidebarLayout extends CustomComponent
{

	private NavigationManager navMan;
	private ViewProvider viewProvider;

	private HorizontalLayout rootContent;
	private BreadCrumbs breadcrumbs;
	private Layout naviContent;
	private TopMenu topMenu;
	private LeftMenu leftMenu;
	private UnityView errorView;

	public static SidebarLayout get(NavigationManager viewMan)
	{
		return new SidebarLayout(viewMan);
	}

	public SidebarLayout(NavigationManager viewMan)
	{
		this.navMan = viewMan;
		setSizeFull();
		setStyleName(Styles.sidebar.toString());

		topMenu = new TopMenu();
		leftMenu = new LeftMenu(viewMan);

	}

	public SidebarLayout build()
	{

		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);

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

		main.addComponent(topMenu);

		rootContent = new HorizontalLayout();
		rootContent.setSizeFull();
		rootContent.setMargin(false);
		rootContent.setSpacing(false);
		rootContent.setStyleName(Styles.rootContent.toString());

		VerticalLayout naviContentWrapper = new VerticalLayout();
		naviContentWrapper.setMargin(false);
		naviContentWrapper.setSpacing(false);
		naviContentWrapper.setSizeFull();
		breadcrumbs = new BreadCrumbs(navMan);
		ui.getNavigator().addViewChangeListener(breadcrumbs);
		naviContentWrapper.addComponents(breadcrumbs, naviContent);
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

	public TopMenu getTopMenu()
	{
		return topMenu;
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
}