/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.leftMenu;

import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import io.imunity.webelements.common.MenuElement;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Simple left menu label
 * 
 * @author P.Piernik
 *
 */
public class LeftMenuLabel extends CustomComponent implements MenuElement
{

	private Label label;
	private HorizontalLayout main;

	public static LeftMenuLabel get()
	{
		return new LeftMenuLabel();
	}

	public LeftMenuLabel()
	{
		main = new HorizontalLayout();
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);
		label = new Label();
		main.addComponent(label);
		main.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		label.setSizeFull();
		label.setCaptionAsHtml(true);
		setStyleName(Styles.menuLabel.toString());
		setCompositionRoot(main);
	}

	public LeftMenuLabel withCaption(String caption)
	{
		label.setCaption(caption);
		return this;
	}

	public LeftMenuLabel withIcon(Resource icon)
	{
		label.setIcon(icon);
		return this;
	}

	public LeftMenuLabel withClickListener(LayoutClickListener listener)
	{
		main.addLayoutClickListener(listener);
		return this;
	}

	@Override
	public void activate()
	{
		setVisible(true);

	}

	@Override
	public void deactivate()
	{
		setVisible(true);

	}

	@Override
	public String getMenuElementId()
	{
		return super.getId();
	}
}