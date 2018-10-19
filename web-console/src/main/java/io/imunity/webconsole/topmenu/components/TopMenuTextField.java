/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.topmenu.components;

import java.util.List;

import com.vaadin.server.Resource;
import com.vaadin.ui.TextField;

import io.imunity.webconsole.layout.MenuComponent;

/**
 * 
 * @author P.Piernik
 *
 */
public class TopMenuTextField extends TextField implements MenuComponent<TextField> {

	public static final String STYLE_NAME = "topMenuTextField";
	
	public static TopMenuTextField get(Resource icon, String placeholder) {
		return new TopMenuTextField(icon, placeholder);
	}
	
	public TopMenuTextField(Resource icon, String placeholder) {
		setIcon(icon);
		setPlaceholder(placeholder);
		addStyleName(STYLE_NAME);
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
	public <C extends MenuComponent<?>> TopMenuTextField remove(C c) {
		return null;
	}

	@Override
	public List<MenuComponent<?>> getList() {
		return null;
	}
}