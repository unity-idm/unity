/*
 * Copyright (c) 2021 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */

package io.imunity.upman.av23.front;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;
import io.imunity.upman.av23.components.Vaddin23WebLogoutHandler;
import io.imunity.upman.av23.front.components.MenuComponent;

import java.util.List;

@CssImport(value = "./styles/vaadin-combo-box.css", themeFor = "vaadin-combo-box")
@CssImport("./styles/views/main-layout.css")
@CssImport("./styles/custom-lumo-theme.css")
@PreserveOnRefresh
public class UnityAppLayout extends FlexLayout implements RouterLayout {

	private final UnityAppLayoutComponentsHolder appLayoutComponents;
	private VerticalLayout rightContainerContent;

	public UnityAppLayout(List<MenuComponent> menuComponents,
	                      Vaddin23WebLogoutHandler authnProcessor) {
		appLayoutComponents = new UnityAppLayoutComponentsHolder(menuComponents, authnProcessor);
	}

	@Override
	public Element getElement() {
		return super.getElement();
	}

	@Override
	public void showRouterLayoutContent(HasElement content) {
		appLayoutComponents.addViewToMainLayout(content);
	}

	protected void initView() {
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

	public void addToLeftContainerAsFirst(Component component) {
		rightContainerContent.addComponentAsFirst(component);
	}
}
