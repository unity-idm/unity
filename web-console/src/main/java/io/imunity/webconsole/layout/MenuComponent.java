/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.layout;

import java.util.List;

import com.vaadin.ui.Component;

/**
 * FIXME: class name -> it is very misleading. MenuComponent might be an element of menu (component of a menu) 
 * or just a menu which is a component. It is very cryptic and as I see in implementation it is both things. 
 * It would be better to split the interface to have two: One which is MenuElement (if needed - I guess not really?)
 * and one which is just a Menu (or MenuContainer). For instance MenuLabel is not a container. And I'm puzzled about MenuButton.
 *  
 * @author P.Piernik
 *
 * @param <T>
 */
public interface MenuComponent<T extends Component> extends Component 
{
	<C extends MenuComponent<?>> C add(C c);
	<C extends MenuComponent<?>> C addAsFirst(C c);
	<C extends MenuComponent<?>> C addAt(C c, int index);
	int count();
	<C extends MenuComponent<?>> T remove(C c);
	List<MenuComponent<?>> getList();
	String getRootStyle();
}