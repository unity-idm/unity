/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.RouterLink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

public class TabComponent extends Tab
{
	public final String name;
	public final List<Class<? extends Component>> componentClass;

	public TabComponent(MenuComponent menu)
	{
		super(new RouterLink(menu.tabName, menu.component));
		Component icon = ofNullable(menu.icon).map(vaadinIcon -> (Component)vaadinIcon.create()).orElseGet(Div::new);
		addComponentAsFirst(icon);
		name = menu.tabName;
		List<Class<? extends Component>> components = new ArrayList<>(List.of(menu.component));
		components.addAll(menu.subViews);
		this.componentClass = Collections.unmodifiableList(components);
	}
}
