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
public class MenuLabel extends CustomComponent implements MenuElement
{

	private Label label;
	private HorizontalLayout main;

	public static MenuLabel get()
	{
		return new MenuLabel();
	}

	public MenuLabel()
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

	public MenuLabel withCaption(String caption)
	{
		label.setCaption(caption);
		return this;
	}

	public MenuLabel withIcon(Resource icon)
	{
		label.setIcon(icon);
		return this;
	}

	public MenuLabel withClickListener(LayoutClickListener listener)
	{
		main.addLayoutClickListener(listener);
		return this;
	}

//	@Override
//	public void setPrimaryStyleName(String style)
//	{
//		label.addStyleName(style);
//	}

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

	

	
	
	
//	@Override
//	public String getRootStyle()
//	{
//		return Styles.menuLabel.toString();
//	}
//
//	@Override
//	public <C extends MenuComponent<?>> C add(C c)
//	{
//		return null;
//	}
//
//	@Override
//	public <C extends MenuComponent<?>> C addAsFirst(C c)
//	{
//		return null;
//	}
//
//	@Override
//	public <C extends MenuComponent<?>> C addAt(C c, int index)
//	{
//		return null;
//	}
//
//	@Override
//	public int count()
//	{
//		return 0;
//	}
//
//	@Override
//	public <C extends MenuComponent<?>> Label remove(C c)
//	{
//		return null;
//	}
//
//	@Override
//	public List<MenuComponent<?>> getList()
//	{
//		return null;
//	}
}