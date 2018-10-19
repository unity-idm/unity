/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.leftmenu.components;

import java.util.List;

import com.vaadin.server.Resource;
import com.vaadin.ui.Label;

import io.imunity.webconsole.layout.MenuComponent;

/**
 * Simple left menu label
 * @author P.Piernik
 *
 */
public class MenuLabel extends Label implements MenuComponent<Label> {
	
	public static final String STYLE_NAME = "menuLabel";
	
	public static MenuLabel get() {
		return new MenuLabel();
	}
	
	public MenuLabel() {
		setCaptionAsHtml(true);
		setPrimaryStyleName(STYLE_NAME);
	}
	
	public MenuLabel withCaption(String caption) {
		super.setCaption(caption);
		return this;
	}
	
	public MenuLabel withIcon(Resource icon) {
		super.setIcon(icon);
		return this;
	}
	
	@Override
	public void setPrimaryStyleName(String style) {
		super.addStyleName(style);
	}

	@Override
	public String getRootStyle() {
		return STYLE_NAME;
	}
	
	@Override
	public <C extends MenuComponent<?>> C add(C c) {
		return null;
	}

	@Override
	public <C extends MenuComponent<?>> C addAsFirst(C c) {
		return null;
	}

	@Override
	public <C extends MenuComponent<?>> C addAt(C c, int index) {
		return null;
	}

	@Override
	public int count() {
		return 0;
	}

	@Override
	public <C extends MenuComponent<?>> MenuLabel remove(C c) {
		return null;
	}

	@Override
	public List<MenuComponent<?>> getList() {
		return null;
	}
}