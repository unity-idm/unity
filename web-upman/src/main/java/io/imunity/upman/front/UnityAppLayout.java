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
import io.imunity.upman.utils.Vaddin23WebLogoutHandler;
import io.imunity.upman.front.components.MenuComponent;

import java.util.List;

@CssImport("./styles/views/main-layout.css")
@CssImport("./styles/custom-lumo-theme.css")
@PreserveOnRefresh
public class UnityAppLayout extends FlexLayout implements RouterLayout
{

	private final UnityAppLayoutComponentsHolder appLayoutComponents;
	private VerticalLayout rightContainerContent;

	public UnityAppLayout(List<MenuComponent> menuComponents,
	                      Vaddin23WebLogoutHandler authnProcessor,
	                      List<Component> additionalIcons)
	{
		appLayoutComponents = new UnityAppLayoutComponentsHolder(menuComponents, authnProcessor, additionalIcons);
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		appLayoutComponents.addViewToMainLayout(content);
	}

	protected void initView()
	{
		setId("unity-layout");

		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setId("unity-main-container");

		rightContainerContent = appLayoutComponents.getLeftContainerWithNavigation();
		rightContainerContent.setId("unity-left-content-container");
		rightContainerContent.getStyle().set("width", null);

		VerticalLayout rightContainerContent = appLayoutComponents.getRightContainerWithNavbarAndViewContent();
		rightContainerContent.setId("unity-right-content-container");

		mainLayout.add(this.rightContainerContent, rightContainerContent);

		add(mainLayout);
	}

	public void addToLeftContainerAsFirst(Component component)
	{
		rightContainerContent.addComponentAsFirst(component);
	}
}
