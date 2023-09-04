/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.dom.Element;
import io.imunity.vaadin.elements.*;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.List;

class UnityAppLayoutComponentsHolder
{
	private final BreadCrumbComponent breadCrumbComponent;
	private final Div viewContainer = new Div();
	private final Tabs tabs;

	private final VaddinWebLogoutHandler authnProcessor;

	private final VerticalLayout leftContainerWithNavigation;
	private final VerticalLayout rightContainerWithNavbarAndViewContent;


	UnityAppLayoutComponentsHolder(List<MenuComponent> menuContent, VaddinWebLogoutHandler authnProcessor, MessageSource msg,
								   List<Component> additionalIcons)
	{
		this.breadCrumbComponent = new BreadCrumbComponent(menuContent, msg::getMessage);
		this.authnProcessor = authnProcessor;
		HorizontalLayout leftNavbarSite = createLeftNavbarSite();
		this.tabs = createLeftMenuTabs(menuContent);

		this.leftContainerWithNavigation = createLeftContainerWithNavigation(tabs);
		this.rightContainerWithNavbarAndViewContent = createRightContainerWithNavbar(createNavbar(leftNavbarSite, createRightNavbarSite(additionalIcons)));

		viewContainer.setHeightFull();
	}

	void reloadBreadCrumb(UnityViewComponent unityViewComponent)
	{
		breadCrumbComponent.update(unityViewComponent);
	}

	VerticalLayout getLeftContainerWithNavigation()
	{
		return leftContainerWithNavigation;
	}

	VerticalLayout getRightContainerWithNavbarAndViewContent()
	{
		return rightContainerWithNavbarAndViewContent;
	}

	void addViewToMainLayout(HasElement content)
	{
		if (content != null)
		{
			Element contentElement = content.getElement();
			setTabsMenu(contentElement);
			viewContainer.getElement().appendChild(contentElement);
		}
	}

	private void setTabsMenu(Element contentElement)
	{
		tabs.setSelectedTab(null);
		contentElement.getComponent().ifPresent(this::selectComponent);
	}

	public void hiddeTextInTabs()
	{
		tabs.getChildren()
				.map(TabTextHider.class::cast)
				.forEach(TabTextHider::hideText);
	}

	public void showTextInTabs()
	{
		tabs.getChildren()
				.map(TabTextHider.class::cast)
				.forEach(TabTextHider::showText);
	}

	private void selectComponent(Component component)
	{
		tabs.getChildren()
				.forEach((Component tab) ->
				{
					if(tab instanceof TabComponent tabComponent)
					{
						if(tabComponent.componentClass.contains(component.getClass()))
							tabs.setSelectedTab(tabComponent);
					}
					if(tab instanceof MultiTabComponent multiTabComponent)
					{
						multiTabComponent.components.stream()
								.filter(x -> x.componentClass.contains(component.getClass()))
								.findFirst()
								.ifPresentOrElse(multiTabComponent::select, () -> multiTabComponent.select(null));
					}

				});
	}

	private HorizontalLayout createNavbar(HorizontalLayout leftNavbarSite, HorizontalLayout rightNavbarSite)
	{
		HorizontalLayout navbarComponent = new HorizontalLayout(leftNavbarSite, rightNavbarSite);
		navbarComponent.setId("unity-navbar");
		return navbarComponent;
	}

	private HorizontalLayout createLeftNavbarSite()
	{
		HorizontalLayout leftNavbar = new HorizontalLayout();
		leftNavbar.setAlignItems(FlexComponent.Alignment.CENTER);
		leftNavbar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		leftNavbar.setSizeFull();
		leftNavbar.add(breadCrumbComponent);
		return leftNavbar;
	}

	private HorizontalLayout createRightNavbarSite(List<Component> additionalIcons)
	{
		Icon logout = createLogoutIcon(authnProcessor::logout);

		HorizontalLayout rightNavbarSite = new HorizontalLayout();
		rightNavbarSite.setAlignItems(FlexComponent.Alignment.CENTER);
		rightNavbarSite.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		rightNavbarSite.setSizeFull();

		additionalIcons.forEach(rightNavbarSite::add);
		rightNavbarSite.add(logout);
		return rightNavbarSite;
	}

	private Icon createLogoutIcon(Runnable logout)
	{
		Icon logoutIcon = new Icon(VaadinIcon.SIGN_OUT);
		logoutIcon.getStyle().set("cursor", "pointer");
		logoutIcon.addClickListener(
				event -> logout.run()
		);
		return logoutIcon;
	}

	private VerticalLayout createLeftContainerWithNavigation(Tabs menuTabsComponent)
	{
		VerticalLayout leftContainer = new VerticalLayout();
		leftContainer.setPadding(false);
		leftContainer.setSpacing(false);
		leftContainer.setAlignItems(FlexComponent.Alignment.STRETCH);
		leftContainer.add(menuTabsComponent);
		return leftContainer;
	}

	private VerticalLayout createRightContainerWithNavbar(HorizontalLayout navbar)
	{
		VerticalLayout rightContainer = new VerticalLayout();
		rightContainer.add(navbar, viewContainer);
		rightContainer.setAlignItems(FlexComponent.Alignment.STRETCH);
		return rightContainer;
	}

	private Tabs createLeftMenuTabs(List<MenuComponent> menuContent)
	{
		Tabs tabs = new Tabs();
		tabs.setOrientation(Tabs.Orientation.VERTICAL);
		tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
		Component[] items = menuContent.stream()
				.map(menu ->
				{
					if(menu.subTabs.isEmpty())
						return new TabComponent(menu);
					else
						return new MultiTabComponent(menu);
				})
				.toArray(Component[]::new);
		tabs.add(items);
		return tabs;
	}
}
