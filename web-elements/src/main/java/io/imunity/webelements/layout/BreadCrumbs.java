/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.layout;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import io.imunity.webelements.common.MenuButton;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationManager;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Breadcrumbs component
 * 
 * @author P.Piernik
 *
 */
public class BreadCrumbs extends CustomComponent implements ViewChangeListener
{

	public static final Images BREADCRUMB_SEPARATOR = Images.rightArrow;

	private HorizontalLayout main;
	private NavigationManager navMan;

	public BreadCrumbs(NavigationManager navMan)
	{
		this.navMan = navMan;
		main = new HorizontalLayout();
		main.setStyleName(Styles.breadcrumbs.toString());
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		main.setSpacing(true);
		setCompositionRoot(main);
	}

	public void adapt(ViewChangeEvent e, List<NavigationInfo> path)
	{
		main.removeAllComponents();

		if (path.isEmpty())
			return;
		// Root
		addElement(path.get(0), e);

		for (NavigationInfo view : path.stream().skip(1).collect(Collectors.toList()))
		{
			addSeparator();
			addElement(view, e);
		}
	}

	private void addElement(NavigationInfo element, ViewChangeEvent e)
	{
		UnityView view = (UnityView) e.getNewView();

		if (element.type == NavigationInfo.Type.View
				|| element.type == NavigationInfo.Type.DefaultView)
		{
			main.addComponent(MenuButton.get(element.id).withNavigateTo(element.id)
					.withCaption(element.caption));

		} else if (element.type == NavigationInfo.Type.ParameterizedView)
		{
			main.addComponent(MenuButton.get(element.id)
					.withCaption(view.getDisplayName()));
		} else
		{
			main.addComponent(MenuButton.get(element.id).withCaption(element.caption));
		}
	}

	private void addSeparator()
	{
		Label s = new Label();
		s.setIcon(BREADCRUMB_SEPARATOR.getResource());
		main.addComponent(s);
		main.setComponentAlignment(s, Alignment.BOTTOM_CENTER);
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