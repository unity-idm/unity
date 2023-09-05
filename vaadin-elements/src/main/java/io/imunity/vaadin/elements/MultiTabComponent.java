/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.List;

@CssImport(value = "./styles/components/multi-tab.css")
@CssImport(value = "./styles/components/multi-tab-details.css", themeFor = "vaadin-details")
@CssImport(value = "./styles/components/vaadin-tooltip-overlay.css", themeFor = "vaadin-tooltip-overlay")
public class MultiTabComponent extends Tab implements TabTextHider
{
	public final MenuComponent menuComponent;
	public final Label label;
	public final Tabs content;
	public final List<TabComponent> components;
	public final Details details;

	public MultiTabComponent(MenuComponent menuComponent)
	{
		details = new Details();
		this.menuComponent = menuComponent;
		label = new Label(menuComponent.tabName);
		label.addComponentAsFirst(menuComponent.icon.create());
		details.setSummary(label);
		components = menuComponent.subTabs.stream()
				.map(TabComponent::new)
				.toList();
		content = new Tabs(components.toArray(TabComponent[]::new));
		content.setOrientation(Tabs.Orientation.VERTICAL);
		content.addThemeVariants(TabsVariant.LUMO_MINIMAL);
		content.setSelectedTab(null);
		details.setContent(content);
		details.addThemeVariants(DetailsVariant.REVERSE);
		details.setClassName("multi-tab-details");
		details.setWidthFull();
		add(details);
		setClassName("multi-tab");
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
		details.addClassName("multi-tab-details-mini");
		Tooltip tooltip = setTooltipText(menuComponent.tabName);
		tooltip.setPosition(Tooltip.TooltipPosition.END_TOP);
	}

	@Override
	public void showText()
	{
		label.setText(menuComponent.tabName);
		label.addComponentAsFirst(menuComponent.icon.create());
		components.forEach(TabComponent::showText);
		details.removeClassName("multi-tab-details-mini");
		setTooltipText(null);
	}
}
