/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.List;

@CssImport(value = "./styles/components/multi-tab.css", themeFor = "vaadin-details")
public class MultiTabComponent extends Details implements TabTextHider
{
	public final MenuComponent menuComponent;
	public final Label label;
	public final Tabs content;
	public final List<TabComponent> components;

	public MultiTabComponent(MenuComponent menuComponent)
	{
		this.menuComponent = menuComponent;
		label = new Label(menuComponent.tabName);
		label.addComponentAsFirst(menuComponent.icon.create());
		setSummary(label);
		components = menuComponent.subTabs.stream()
				.map(TabComponent::new)
				.toList();
		content = new Tabs(components.toArray(TabComponent[]::new));
		content.setOrientation(Tabs.Orientation.VERTICAL);
		content.addThemeVariants(TabsVariant.LUMO_MINIMAL);
		content.setSelectedTab(null);
		setContent(content);
		addThemeVariants(DetailsVariant.REVERSE);
		setClassName("multi-tab");
		getStyle().set("margin", "0");
	}

	public void select(TabComponent tabComponent)
	{
		if(tabComponent != null)
			setOpened(true);
		content.setSelectedTab(tabComponent);
	}

	@Override
	public void hiddeText()
	{
		label.setText("");
		label.addComponentAsFirst(menuComponent.icon.create());
		components.forEach(TabComponent::hiddeText);
		addClassName("multi-tab-mini");
		getElement().setProperty("title", menuComponent.tabName);
	}

	@Override
	public void showText()
	{
		label.setText(menuComponent.tabName);
		label.addComponentAsFirst(menuComponent.icon.create());
		components.forEach(TabComponent::showText);
		removeClassName("multi-tab-mini");
		getElement().removeProperty("title");
	}
}
