/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webelements.navigation;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import io.imunity.webelements.menu.MenuButton;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Layout which contains breadcrumbs elements
 * @author P.Piernik
 *
 */
public class BreadcrumbsComponent extends HorizontalLayout
{
	public static final Images BREADCRUMB_SEPARATOR = Images.rightArrow;
	
	public BreadcrumbsComponent()
	{
		setWidth(100, Unit.PERCENTAGE);
		setHeightUndefined();
		setMargin(false);
		setSpacing(true);
	}
	
	public void clear()
	{
		removeAllComponents();
	}
	
	public void addSeparator()
	{
		Label s = new Label(BREADCRUMB_SEPARATOR.getHtml(), ContentMode.HTML);
		addComponent(s);
	}
	
	public void addButton(MenuButton button)
	{
		addComponent(button);
	}

	public void addSubBreadcrumbs(BreadcrumbsComponent breadcrumbsComponent)
	{
		addComponent(breadcrumbsComponent);	
	}
}
