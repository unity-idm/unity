/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.layout;

import java.util.List;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.Dashboard;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Main layout of Webconsole endpoint
 * 
 * @author P.Piernik
 *
 */
public class WebConsoleLayout extends VerticalLayout
{

	private DefaultViewChangeManager viewChangeManager;
	private ViewProvider viewProvider;
	
	private HorizontalLayout rootContent;
	private BreadCrumbs breadcrumbs;
	private Layout naviContent;
	private TopMenu topMenu;
	private LeftMenu leftMenu;
	

	public static WebConsoleLayout get()
	{
		return new WebConsoleLayout();
	}

	public WebConsoleLayout()
	{
		setSizeFull();
		setStyleName(Styles.webConsole.toString());
		setMargin(false);
		setSpacing(false);
		topMenu = new TopMenu();
		leftMenu = new LeftMenu();
		viewChangeManager = new DefaultViewChangeManager();
	}

	public WebConsoleLayout build()
	{
		UI ui = UI.getCurrent();

		if (naviContent == null)
		{
			naviContent = new VerticalLayout();
		}
		naviContent.setSizeFull();
		naviContent.setStyleName(Styles.contentBox.toString());

		new Navigator(ui, naviContent);
		ui.getNavigator().setErrorView(Dashboard.class);
		ui.getNavigator().addProvider(viewProvider);

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
				List<MenuComponent<?>> menuContentList = viewChangeManager
						.init(WebConsoleLayout.this);
				viewChangeManager.manage(WebConsoleLayout.this, leftMenu, event,
						menuContentList);
				viewChangeManager.finish(WebConsoleLayout.this, menuContentList);
			}
		});

		addComponent(topMenu);

		rootContent = new HorizontalLayout();
		rootContent.setSizeFull();
		rootContent.setMargin(false);
		rootContent.setSpacing(false);
		rootContent.setStyleName(Styles.rootContent.toString());

		VerticalLayout naviContentWrapper = new VerticalLayout();
		naviContentWrapper.setMargin(false);
		naviContentWrapper.setSpacing(false);
		naviContentWrapper.setSizeFull();
		breadcrumbs = new BreadCrumbs();
		naviContentWrapper.addComponents(breadcrumbs, naviContent);
		naviContentWrapper.setExpandRatio(naviContent, 1f);

		rootContent.addComponents(leftMenu, naviContentWrapper);
		rootContent.setExpandRatio(naviContentWrapper, 1f);

		addComponent(rootContent);
		setExpandRatio(rootContent, 1f);

		addStyleName(Styles.wbWhite.toString());

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

	public WebConsoleLayout withNaviContent(Layout naviRootContent)
	{
		this.naviContent = naviRootContent;
		return this;
	}

	public WebConsoleLayout withViewProvider(ViewProvider provider)
	{
		this.viewProvider = provider;
		return this;
	}

	public void setViewChangeManager(DefaultViewChangeManager viewChangeManager)
	{
		this.viewChangeManager = viewChangeManager;
	}

}