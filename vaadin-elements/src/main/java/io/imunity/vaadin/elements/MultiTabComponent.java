/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.List;

public class MultiTabComponent extends Tab implements TabTextHider
{
	public final MenuComponent menuComponent;
	public final Span label;
	public final Tabs content;
	public final List<TabComponent> components;
	public final Details details;

	public MultiTabComponent(MenuComponent menuComponent)
	{
		details = new Details();
		this.menuComponent = menuComponent;
		label = new Span(menuComponent.tabName);
		label.addComponentAsFirst(menuComponent.icon.create());
		details.setSummary(label);
		components = menuComponent.subTabs.stream()
				.map(TabComponent::new)
				.toList();
		content = new Tabs(components.toArray(TabComponent[]::new));
		content.setOrientation(Tabs.Orientation.VERTICAL);
		content.addThemeVariants(TabsVariant.LUMO_MINIMAL);
		content.setSelectedTab(null);
		content.addClassName("u-menu-tabs");
		details.setContent(content);
		details.addThemeVariants(DetailsVariant.REVERSE);
		details.addClassName("u-multi-tab-details");
		details.setWidthFull();
		add(details);
		setClassName("u-multi-tab");
	}

	public void select(TabComponent tabComponent)
	{
		if(tabComponent != null)
			details.setOpened(true);
		content.setSelectedTab(tabComponent);
	}

	@Override
	public void hideText()
	{
		label.setText("");
		label.addComponentAsFirst(menuComponent.icon.create());
		components.forEach(TabComponent::hideText);
		details.addClassName("u-multi-tab-details-mini");
		Tooltip tooltip = setTooltipText(menuComponent.tabName);
		tooltip.setPosition(Tooltip.TooltipPosition.END_TOP);
	}

	@Override
	public void showText()
	{
		label.setText(menuComponent.tabName);
		label.addComponentAsFirst(menuComponent.icon.create());
		components.forEach(TabComponent::showText);
		details.removeClassName("u-multi-tab-details-mini");
		setTooltipText(null);
	}
}
