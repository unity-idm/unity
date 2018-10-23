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

/**
 * Main layout of Webconsole endpoint
 * @author P.Piernik
 *
 */
public class WebConsoleLayout extends VerticalLayout {
	
	public static final String STYLE_NAME = "webConsole";
	
	private DefaultViewChangeManager viewChangeManager = new DefaultViewChangeManager();
	private boolean buildRunning = false;

	private HorizontalLayout content = new HorizontalLayout();
	
	private BreadCrumbs breadcrumbs = null;
	private Layout naviRootContent = null;
	private VerticalLayout rootContent = new VerticalLayout();
	private TopMenu topMenu = new TopMenu();
	private LeftMenu leftMenu = new LeftMenu();
	private ViewProvider viewProvider;

	public static WebConsoleLayout get() {
		return new WebConsoleLayout();
	}
	
	public WebConsoleLayout() {
		super();
		setSizeFull();
		setStyleName(STYLE_NAME);
		setMargin(false);
		setSpacing(false);
	}

	public WebConsoleLayout build()
	{
		if (!buildRunning)
		{
			UI ui = UI.getCurrent();

			if (naviRootContent == null)
			{
				naviRootContent = new VerticalLayout();
			}

		//	naviRootContent.setWidth(100, Unit.PERCENTAGE);
			naviRootContent.setSizeFull();
			naviRootContent.setStyleName("contentBox");

			new Navigator(ui, naviRootContent);
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

			content.setSizeFull();
			content.setMargin(false);
			content.setSpacing(false);
			content.setStyleName("rootContent");
			addComponent(content);
			setExpandRatio(content, 1f);

			content.addComponents(leftMenu, rootContent);
			content.setExpandRatio(rootContent, 1f);

			rootContent.setMargin(false);
			rootContent.setSpacing(false);
			rootContent.setSizeFull();
			breadcrumbs = new BreadCrumbs();
			rootContent.addComponent(breadcrumbs);
			rootContent.addComponent(naviRootContent);
			rootContent.setExpandRatio(naviRootContent, 1f);

			addStyleName("white");
			//VaadinSession.getCurrent().setAttribute(WebConsoleLayout.class, this);
			buildRunning = true;
		}
		return this;
	}
	
	public VerticalLayout getRootContent() {
		return rootContent;
	}
	
	public LeftMenu getLeftMenu() {
		return leftMenu;
	}
	
	public TopMenu getTopMenu() {
		return topMenu;
	}
	
	public BreadCrumbs getBreadCrumbs() {
		return breadcrumbs;
	}
	

	public Layout getNaviContent() {
		return naviRootContent;
	}
	
	public WebConsoleLayout withNaviContent(Layout naviRootContent) {
		this.naviRootContent = naviRootContent;
		return this;
	}
	
	public WebConsoleLayout withViewProvider(ViewProvider provider)
	{
		this.viewProvider = provider;
		return this;
	}
	
	public void setViewChangeManager(DefaultViewChangeManager viewChangeManager) {
		this.viewChangeManager = viewChangeManager;
	}



}