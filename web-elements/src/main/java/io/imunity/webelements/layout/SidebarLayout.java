/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.layout;

import java.util.ArrayList;
import java.util.Collection;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.common.DefaultErrorView;
import io.imunity.webelements.leftMenu.LeftMenu;
import io.imunity.webelements.navigation.NavigationManager;
import io.imunity.webelements.topMenu.TopMenu;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Main layout of Webconsole endpoint
 * 
 * @author P.Piernik
 *
 */
public class SidebarLayout extends CustomComponent
{

	private NavigationManager viewMan;
	private Collection<ViewProvider> viewProviders;

	private HorizontalLayout rootContent;
	private BreadCrumbs breadcrumbs;
	private Layout naviContent;
	private TopMenu topMenu;
	private LeftMenu leftMenu;

	public static SidebarLayout get(NavigationManager viewMan)
	{
		return new SidebarLayout(viewMan);
	}

	public SidebarLayout(NavigationManager viewMan)
	{
		this.viewMan = viewMan;
		this.viewProviders = new ArrayList<>();
		setSizeFull();
		setStyleName(Styles.webConsole.toString());

		topMenu = new TopMenu();
		leftMenu = new LeftMenu();

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
		ui.getNavigator().setErrorView(DefaultErrorView.class);
		for (ViewProvider p : viewProviders)
		{
			ui.getNavigator().addProvider(p);
		}
		ui.getNavigator().addViewChangeListener(new ViewChangeListener()
		{
			@Override
			public boolean beforeViewChange(ViewChangeEvent event)
			{
				return true;
			}

			@Override
			public void afterViewChange(ViewChangeEvent event)
			{

				leftMenu.adapt(viewMan.getParentPath(event.getViewName()));

			}
		});

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
		breadcrumbs = new BreadCrumbs(viewMan);
		ui.getNavigator().addViewChangeListener(breadcrumbs);
		naviContentWrapper.addComponents(breadcrumbs, naviContent);
		naviContentWrapper.setExpandRatio(naviContent, 1f);

		rootContent.addComponents(leftMenu, naviContentWrapper);
		rootContent.setExpandRatio(naviContentWrapper, 1f);

		main.addComponent(rootContent);
		main.setExpandRatio(rootContent, 1f);

		addStyleName(Styles.wbWhite.toString());
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
		viewProviders.add(provider);
		return this;
	}
}