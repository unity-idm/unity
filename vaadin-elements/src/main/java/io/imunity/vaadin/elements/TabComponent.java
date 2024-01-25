/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.RouterLink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;

public class TabComponent extends Tab implements TabTextHider, ClickNotifier<TabComponent>
{
	public final String name;
	public final List<Class<? extends Component>> componentClass;
	private final RouterLink routerLink;
	private final Component icon;

	public TabComponent(MenuComponent menu)
	{
		this.routerLink = new RouterLink(menu.tabName, menu.component);
		routerLink.setClassName("u-tab-component-link");
		this.icon = ofNullable(menu.icon).map(vaadinIcon -> (Component)vaadinIcon.create()).orElseGet(Div::new);
		add(routerLink);
		routerLink.addComponentAsFirst(icon);
		name = menu.tabName;
		List<Class<? extends Component>> components = new ArrayList<>(List.of(menu.component));
		components.addAll(menu.subViews);
		this.componentClass = Collections.unmodifiableList(components);
	}

	@Override
	public void hideText()
	{
		routerLink.setText("");
		routerLink.addComponentAsFirst(icon);
		setTooltipText(name).setPosition(Tooltip.TooltipPosition.END);
	}

	@Override
	public void showText()
	{
		routerLink.setText(name);
		routerLink.addComponentAsFirst(icon);
		setTooltipText(null);
	}
}
