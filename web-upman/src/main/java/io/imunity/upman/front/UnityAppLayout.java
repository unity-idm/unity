/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;
import io.imunity.vaadin23.elements.ExtraLayoutPanel;
import io.imunity.vaadin23.elements.MenuComponent;
import io.imunity.vaadin23.endpoint.common.Vaadin823EndpointProperties;
import io.imunity.vaadin23.endpoint.common.Vaddin23WebLogoutHandler;

import java.util.List;

import static io.imunity.vaadin23.endpoint.common.Vaadin23WebAppContext.getCurrentWebAppVaadinProperties;

@CssImport("./styles/views/main-layout.css")
@CssImport("./styles/custom-lumo-theme.css")
@PreserveOnRefresh
public class UnityAppLayout extends FlexLayout implements RouterLayout
{

	private final UnityAppLayoutComponentsHolder appLayoutComponents;
	private final Vaadin823EndpointProperties vaadinEndpointProperties;
	private VerticalLayout leftContainerContent;

	public UnityAppLayout(List<MenuComponent> menuComponents,
	                      Vaddin23WebLogoutHandler authnProcessor,
	                      List<Component> additionalIcons)
	{
		appLayoutComponents = new UnityAppLayoutComponentsHolder(menuComponents, authnProcessor, additionalIcons);
		vaadinEndpointProperties = getCurrentWebAppVaadinProperties();
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		appLayoutComponents.addViewToMainLayout(content);
	}

	protected void initView()
	{
		setId("unity-layout");

		ExtraLayoutPanel top = new ExtraLayoutPanel("unity-layout-top", vaadinEndpointProperties.getExtraTopPanel().orElse(null));
		ExtraLayoutPanel left = new ExtraLayoutPanel("unity-layout-left", vaadinEndpointProperties.getExtraLeftPanel().orElse(null));
		ExtraLayoutPanel right = new ExtraLayoutPanel("unity-layout-right", vaadinEndpointProperties.getExtraRightPanel().orElse(null));
		ExtraLayoutPanel bottom = new ExtraLayoutPanel("unity-layout-bottom", vaadinEndpointProperties.getExtraBottomPanel().orElse(null));


		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setId("unity-main-container");

		leftContainerContent = appLayoutComponents.getLeftContainerWithNavigation();
		leftContainerContent.setId("unity-left-content-container");
		leftContainerContent.getStyle().set("width", null);

		VerticalLayout rightContainerContent = appLayoutComponents.getRightContainerWithNavbarAndViewContent();
		rightContainerContent.setId("unity-right-content-container");

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
}
