/*
 * Copyright (c) 2021 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.upman.av23.front;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.dom.Element;
import io.imunity.upman.av23.components.Vaddin23WebLogoutHandler;
import io.imunity.upman.av23.front.components.MenuComponent;
import io.imunity.upman.av23.front.components.NavbarIconFactory;
import io.imunity.upman.av23.front.components.TabComponent;

import java.util.List;
import java.util.Optional;

class UnityAppLayoutComponentsHolder
{
	private final Div viewContainer = new Div();
	private final HorizontalLayout leftNavbarSite;
	private final Tabs tabs;

	private final Vaddin23WebLogoutHandler authnProcessor;

	private final VerticalLayout leftContainerWithNavigation;
	private final VerticalLayout rightContainerWithNavbarAndViewContent;


	UnityAppLayoutComponentsHolder(List<MenuComponent> menuContent, Vaddin23WebLogoutHandler authnProcessor) {
		this.authnProcessor = authnProcessor;
		this.leftNavbarSite = createLeftNavbarSite();
		this.tabs = createLeftMenuTabs(menuContent);

		this.leftContainerWithNavigation = createLeftContainerWithNavigation(tabs);
		this.rightContainerWithNavbarAndViewContent = createRightContainerWithNavbar(createNavbar(leftNavbarSite, createRightNavbarSite()));

		viewContainer.setHeightFull();
	}

	VerticalLayout getLeftContainerWithNavigation() {
		return leftContainerWithNavigation;
	}

	VerticalLayout getRightContainerWithNavbarAndViewContent() {
		return rightContainerWithNavbarAndViewContent;
	}

	void addViewToMainLayout(HasElement content) {
		if (content != null) {
			Element contentElement = content.getElement();
			setTabsMenu(contentElement);
			viewContainer.getElement().appendChild(contentElement);
		}
	}

	private void setTabsMenu(Element contentElement)
	{
		tabs.setSelectedTab(null);
		contentElement.getComponent().flatMap(this::findTabForComponent).ifPresent(tabs::setSelectedTab);
	}

	private Optional<TabComponent> findTabForComponent(Component component) {
		return tabs.getChildren()
				.map(TabComponent.class::cast)
				.filter(tab -> tab.componentClass.contains(component.getClass()))
				.findFirst();
	}

	private HorizontalLayout createNavbar(HorizontalLayout leftNavbarSite, HorizontalLayout rightNavbarSite) {
		HorizontalLayout navbarComponent = new HorizontalLayout(leftNavbarSite, rightNavbarSite);
		navbarComponent.setId("unity-navbar");
		return navbarComponent;
	}

	private HorizontalLayout createLeftNavbarSite() {
		HorizontalLayout leftNavbar = new HorizontalLayout();
		leftNavbar.setAlignItems(FlexComponent.Alignment.CENTER);
		leftNavbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		leftNavbar.setSizeFull();
		return leftNavbar;
	}

	private HorizontalLayout createRightNavbarSite() {
		Icon home = NavbarIconFactory.homeCreate();
		Icon logout = NavbarIconFactory.logoutCreate(authnProcessor::logout);

		HorizontalLayout rightNavbarSite = new HorizontalLayout();
		rightNavbarSite.setAlignItems(FlexComponent.Alignment.CENTER);
		rightNavbarSite.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		rightNavbarSite.setSizeFull();

		rightNavbarSite.add(home, logout);
		return rightNavbarSite;
	}

	private VerticalLayout createLeftContainerWithNavigation(Tabs menuTabsComponent) {
		VerticalLayout leftContainer = new VerticalLayout();
		leftContainer.setPadding(false);
		leftContainer.setSpacing(false);
		leftContainer.setAlignItems(FlexComponent.Alignment.STRETCH);
		leftContainer.add(menuTabsComponent);
		return leftContainer;
	}

	private VerticalLayout createRightContainerWithNavbar(HorizontalLayout navbar) {
		VerticalLayout rightContainer = new VerticalLayout();
		rightContainer.add(navbar, viewContainer);
		rightContainer.setAlignItems(FlexComponent.Alignment.STRETCH);
		return rightContainer;
	}

	private Tabs createLeftMenuTabs(List<MenuComponent> menuContent) {
		Tabs tabs = new Tabs();
		tabs.setOrientation(Tabs.Orientation.VERTICAL);
		tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
		Component[] items = menuContent.stream()
				.map(TabComponent::new)
				.toArray(Tab[]::new);
		tabs.add(items);
		tabs.addSelectedChangeListener(event -> {
			TabComponent selectedTab;
			if(event.getSelectedTab() == null)
				selectedTab = (TabComponent) event.getPreviousTab();
			else
				selectedTab = (TabComponent) event.getSelectedTab();

			leftNavbarSite.removeAll();
			leftNavbarSite.add(new Label(selectedTab.name));
		});
		return tabs;
	}

}
