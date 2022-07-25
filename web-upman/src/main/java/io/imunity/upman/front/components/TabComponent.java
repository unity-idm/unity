/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.front.components;

import com.google.common.collect.Lists;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.RouterLink;

import java.util.Collections;
import java.util.List;

public class TabComponent extends Tab
{
	public final String name;
	public final List<Class<? extends Component>> componentClass;

	public TabComponent(MenuComponent menu)
	{
		super(menu.icon.create(), new RouterLink(menu.tabName, menu.component));
		name = menu.tabName;
		List<Class<? extends Component>> components = Lists.newArrayList(menu.component);
		components.addAll(menu.subViews);
		this.componentClass = Collections.unmodifiableList(components);
	}
}
