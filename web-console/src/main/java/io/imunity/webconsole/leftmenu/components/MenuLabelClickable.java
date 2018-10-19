/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.leftmenu.components;

import java.util.List;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import io.imunity.webconsole.layout.MenuComponent;

/**
 * Clickable label
 * @author P.Piernik
 *
 */
public class MenuLabelClickable extends HorizontalLayout implements MenuComponent<Label>
{
	public static final String STYLE_NAME = "menuLabel";

	private Label label;

	public static MenuLabelClickable get()
	{
		return new MenuLabelClickable();
	}

	public MenuLabelClickable()
	{
		setSizeFull();
		setMargin(false);
		setSpacing(false);
		label = new Label();
		addComponent(label);
		setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		label.setSizeFull();
		label.setCaptionAsHtml(true);
		setPrimaryStyleName(STYLE_NAME);
	}

	public MenuLabelClickable withCaption(String caption)
	{
		label.setCaption(caption);
		return this;
	}

	public MenuLabelClickable withIcon(Resource icon)
	{
		label.setIcon(icon);
		return this;
	}

	@Override
	public void setPrimaryStyleName(String style)
	{
		label.addStyleName(style);
	}

	@Override
	public String getRootStyle()
	{
		return STYLE_NAME;
	}

	@Override
	public <C extends MenuComponent<?>> C add(C c)
	{
		return null;
	}

	@Override
	public <C extends MenuComponent<?>> C addAsFirst(C c)
	{
		return null;
	}

	@Override
	public <C extends MenuComponent<?>> C addAt(C c, int index)
	{
		return null;
	}

	@Override
	public int count()
	{
		return 0;
	}

	@Override
	public <C extends MenuComponent<?>> MenuLabel remove(C c)
	{
		return null;
	}

	@Override
	public List<MenuComponent<?>> getList()
	{
		return null;
	}
}
