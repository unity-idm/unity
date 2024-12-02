/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.layout;

import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;
import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE_SMALL;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppVaadinProperties;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;

import io.imunity.vaadin.elements.AfterSubNavigationEvent;
import io.imunity.vaadin.elements.MenuComponent;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.api.EnquiresDialogLauncher;
import pl.edu.icm.unity.base.message.MessageSource;



public class LeftNavbarAppLayout extends FlexLayout implements RouterLayout, AfterNavigationObserver
{

	private final UnityAppLayoutComponentsHolder appLayoutComponents;
	private final VaadinEndpointProperties vaadinEndpointProperties;
	private final ExtraPanelsConfigurationProvider extraPanelsConfiguration;
	private final EnquiresDialogLauncher enquiresDialogLauncher;
	private final boolean showEnquiry;
	private boolean enquiryChecked = false;
	
	
	private VerticalLayout leftContainerContent;

	
	public LeftNavbarAppLayout(List<MenuComponent> menuComponents,
	                      VaadinWebLogoutHandler authnProcessor,
						  MessageSource msg,
						  EnquiresDialogLauncher enquiresDialogLauncher,
						  boolean showEnquiry,
	                      List<Component> additionalIcons, 
	                      ExtraPanelsConfigurationProvider extraPanelsConfiguration)
	{
		appLayoutComponents = new UnityAppLayoutComponentsHolder(menuComponents, authnProcessor, msg, additionalIcons);
		vaadinEndpointProperties = getCurrentWebAppVaadinProperties();
		this.extraPanelsConfiguration = extraPanelsConfiguration;
		this.enquiresDialogLauncher = enquiresDialogLauncher;
		this.showEnquiry = showEnquiry;
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		if (!showEnquiry)
		{
			appLayoutComponents.addViewToMainLayout(content);
		} else
		{
			if (!enquiryChecked)
			{
				enquiresDialogLauncher
						.showEnquiryDialogIfNeeded(() -> appLayoutComponents.addViewToMainLayout(content));
			} else
			{
				appLayoutComponents.addViewToMainLayout(content);
			}
			enquiryChecked = true;
		}
	}

	protected void initView()
	{
		setClassName("u-main-layout");
		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setClassName("u-main-layout-container");

		leftContainerContent = appLayoutComponents.getLeftContainerWithNavigation();
		leftContainerContent.setClassName("u-main-layout-left-container");
		leftContainerContent.getStyle().set("width", null);

		VerticalLayout rightContainerContent = appLayoutComponents.getRightContainerWithNavbarAndViewContent();
		rightContainerContent.setClassName("u-main-layout-right-container");

		HorizontalLayout horizontalLayout = new HorizontalLayout(this.leftContainerContent, rightContainerContent);
		horizontalLayout.getStyle().set("gap", "0");
		horizontalLayout.setWidthFull();
		UnityLayoutWrapper.wrap(this, horizontalLayout, vaadinEndpointProperties, extraPanelsConfiguration, false);
		
	}

	public void addToLeftContainerAsFirst(Component component)
	{
		leftContainerContent.addComponentAsFirst(component);
	}

	public void activateLeftContainerMinimization(Image image)
	{
		VerticalLayout verticalLayout = new VerticalLayout();
		Icon leftArrow = VaadinIcon.ANGLE_DOUBLE_LEFT.create();
		Icon rightArrow = VaadinIcon.ANGLE_DOUBLE_RIGHT.create();
		rightArrow.setVisible(false);
		leftArrow.addClickListener(event ->
		{
			leftArrow.setVisible(false);
			rightArrow.setVisible(true);
			leftContainerContent.getStyle().set("width", "4em");
			image.setClassName(LOGO_IMAGE_SMALL.getName());
			verticalLayout.setAlignItems(Alignment.CENTER);
			appLayoutComponents.hiddeTextInTabs();
		});
		rightArrow.addClickListener(event ->
		{
			leftArrow.setVisible(true);
			rightArrow.setVisible(false);
			leftContainerContent.getStyle().remove("width");
			image.setClassName(LOGO_IMAGE.getName());
			verticalLayout.setAlignItems(Alignment.END);
			appLayoutComponents.showTextInTabs();

		});
		verticalLayout.add(leftArrow, rightArrow);
		verticalLayout.setAlignItems(Alignment.END);
		verticalLayout.setJustifyContentMode(JustifyContentMode.END);
		verticalLayout.setHeightFull();
		verticalLayout.setClassName("u-main-layout-left-minimization-container");
		leftContainerContent.add(verticalLayout);
		ComponentUtil.addListener(UI.getCurrent(), AfterSubNavigationEvent.class, event -> appLayoutComponents.reloadBreadCrumb(event.getSource()));
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event)
	{
		if(event.getActiveChain().iterator().next() instanceof UnityViewComponent viewComponent)
			appLayoutComponents.reloadBreadCrumb(viewComponent);
	}
}
