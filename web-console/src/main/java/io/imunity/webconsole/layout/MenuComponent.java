/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.layout;

import java.util.List;

import com.vaadin.ui.Component;

/**
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public interface MenuComponent<T extends Component> extends Component {
	public <C extends MenuComponent<?>> C add(C c);
	public <C extends MenuComponent<?>> C addAsFirst(C c);
	public <C extends MenuComponent<?>> C addAt(C c, int index);
	public int count();
	public <C extends MenuComponent<?>> T remove(C c);
	public List<MenuComponent<?>> getList();
	public String getRootStyle();
}