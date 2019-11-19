/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.CustomComponent;

import io.imunity.webelements.menu.MenuButton;
import io.imunity.webelements.navigation.BreadcrumbsComponent;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.UnityView;
import io.imunity.webelements.navigation.UnityViewWithSubViews;
import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Breadcrumbs component
 * 
 * @author P.Piernik
 *
 */
public class WebConsoleBreadCrumbs extends CustomComponent implements ViewChangeListener
{
	private BreadcrumbsComponent main;
	private NavigationHierarchyManager navMan;

	public WebConsoleBreadCrumbs(NavigationHierarchyManager navMan)
	{
		this.navMan = navMan;
		main = new BreadcrumbsComponent();
		setCompositionRoot(main);
		setStyleName(SidebarStyles.breadcrumbs.toString());
	}

	public void adapt(ViewChangeEvent e, List<NavigationInfo> path)
	{
		main.clear();

		if (path.isEmpty())
			return;
		// Root
		addElement(path.get(0), e);

		for (NavigationInfo view : path.stream().skip(1).collect(Collectors.toList()))
		{
			main.addSeparator();
			addElement(view, e);
		}
	}

	private void addElement(NavigationInfo element, ViewChangeEvent e)
	{
		UnityView view = (UnityView) e.getNewView();

		if (element.type == NavigationInfo.Type.View
				|| element.type == NavigationInfo.Type.DefaultView)
		{
			main.addButton(MenuButton.get(element.id).withNavigateTo(element.id)
					.withCaption(element.caption).clickable());

		} else if (element.type == NavigationInfo.Type.ParameterizedView)
		{
			main.addButton(MenuButton.get(element.id)
					.withCaption(view.getDisplayedName()));
		} else if (element.type == NavigationInfo.Type.ParameterizedViewWithSubviews)
		{
			UnityViewWithSubViews viewWithSubViews = (UnityViewWithSubViews) view;
			main.addSubBreadcrumbs(viewWithSubViews.getBreadcrumbsComponent());
			
		} else
		{
			main.addButton(MenuButton.get(element.id).withCaption(element.caption));
		}
	}

	@Override
	public boolean beforeViewChange(ViewChangeEvent event)
	{

		return true;
	}

	@Override
	public void afterViewChange(ViewChangeEvent event)
	{

		adapt(event, navMan.getParentPath(event.getViewName()));
	}

}