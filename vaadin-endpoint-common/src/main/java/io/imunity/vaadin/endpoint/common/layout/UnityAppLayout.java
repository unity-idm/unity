/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.layout;

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
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;
import io.imunity.vaadin.elements.AfterSubNavigationEvent;
import io.imunity.vaadin.elements.ExtraLayoutPanel;
import io.imunity.vaadin.elements.MenuComponent;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.List;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppVaadinProperties;


@PreserveOnRefresh
public class UnityAppLayout extends FlexLayout implements RouterLayout, AfterNavigationObserver
{

	private final UnityAppLayoutComponentsHolder appLayoutComponents;
	private final Vaadin82XEndpointProperties vaadinEndpointProperties;
	private VerticalLayout leftContainerContent;

	public UnityAppLayout(List<MenuComponent> menuComponents,
	                      VaddinWebLogoutHandler authnProcessor,
						  MessageSource msg,
	                      List<Component> additionalIcons)
	{
		appLayoutComponents = new UnityAppLayoutComponentsHolder(menuComponents, authnProcessor, msg, additionalIcons);
		vaadinEndpointProperties = getCurrentWebAppVaadinProperties();
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		appLayoutComponents.addViewToMainLayout(content);
	}

	protected void initView()
	{
		setClassName("u-main-layout");

		ExtraLayoutPanel top = new ExtraLayoutPanel("unity-layout-top", vaadinEndpointProperties.getExtraTopPanel().orElse(null));
		ExtraLayoutPanel left = new ExtraLayoutPanel("unity-layout-left", vaadinEndpointProperties.getExtraLeftPanel().orElse(null));
		ExtraLayoutPanel right = new ExtraLayoutPanel("unity-layout-right", vaadinEndpointProperties.getExtraRightPanel().orElse(null));
		ExtraLayoutPanel bottom = new ExtraLayoutPanel("unity-layout-bottom", vaadinEndpointProperties.getExtraBottomPanel().orElse(null));


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

		mainLayout.add(left, horizontalLayout, right);

		add(top, mainLayout, bottom);
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
			image.getStyle().set("max-width", "2.5em");
			verticalLayout.setAlignItems(Alignment.CENTER);
			appLayoutComponents.hiddeTextInTabs();
		});
		rightArrow.addClickListener(event ->
		{
			leftArrow.setVisible(true);
			rightArrow.setVisible(false);
			leftContainerContent.getStyle().remove("width");
			image.getStyle().set("max-width", "5rem");
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
