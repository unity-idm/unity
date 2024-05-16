/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views;

import static io.imunity.vaadin.elements.CSSVars.MEDIUM_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppContextProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.ProjectManagementHelper;
import io.imunity.home.views.profile.ProfileView;
import io.imunity.home.views.sign_in.SignInView;
import io.imunity.home.views.trusted_application.TrustedApplicationsView;
import io.imunity.home.views.trusted_device.TrustedDeviceView;
import io.imunity.vaadin.elements.MenuComponent;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.layout.ExtraPanelsConfiguration;
import io.imunity.vaadin.endpoint.common.layout.LeftNavbarAppLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;

public class HomeUiMenu extends LeftNavbarAppLayout implements BeforeEnterObserver
{
	private final static int imageSize = 7;
	private final AttributesManagement attributesMan;
	private final AttributeHandlerRegistry registry;
	private final HomeEndpointProperties homeEndpointProperties;

	@Autowired
	public HomeUiMenu(VaddinWebLogoutHandler standardWebLogoutHandler, MessageSource msg,
					  ProjectManagementHelper projectManagementHelper, AttributesManagement attributesMan,
					  AttributeHandlerRegistry registry, ExtraPanelsConfiguration extraPanelsConfiguration)
	{
		super(createMenuComponents(msg), standardWebLogoutHandler, msg, List.of(createLoggedAsLabel(msg), createUpmanIcon(projectManagementHelper)), extraPanelsConfiguration);
		this.attributesMan = attributesMan;
		this.registry = registry;
		this.homeEndpointProperties = new HomeEndpointProperties(getCurrentWebAppContextProperties());
		ComponentUtil.setData(UI.getCurrent(), HomeEndpointProperties.class, homeEndpointProperties);

		HorizontalLayout imageLayout = createImageLayout(homeEndpointProperties);

		super.initView();

		addToLeftContainerAsFirst(imageLayout);
	}

	private static List<MenuComponent> createMenuComponents(MessageSource msg)
	{
		HomeEndpointProperties homeEndpointProperties = new HomeEndpointProperties(getCurrentWebAppContextProperties());
		Set<String> disabled = homeEndpointProperties.getDisabledComponents();
		List<MenuComponent> menuComponents = new ArrayList<>();

		if (!disabled.contains(HomeEndpointProperties.Components.userDetailsTab.toString()))
			menuComponents.add(MenuComponent.builder(ProfileView.class).tabName(msg.getMessage("UserHomeUI.profile")).build());
		if (!disabled.contains(HomeEndpointProperties.Components.credentialTab.toString()))
			menuComponents.add(MenuComponent.builder(SignInView.class).tabName(msg.getMessage("UserHomeUI.signIn")).build());
		if (!disabled.contains(HomeEndpointProperties.Components.trustedApplications.toString()))
			menuComponents.add(MenuComponent.builder(TrustedApplicationsView.class).tabName(msg.getMessage("UserHomeUI.trustedApplications")).build());
		if (!disabled.contains(HomeEndpointProperties.Components.trustedDevices.toString()))
			menuComponents.add(MenuComponent.builder(TrustedDeviceView.class).tabName(msg.getMessage("UserHomeUI.trustedDevices")).build());
		if (!disabled.contains(HomeEndpointProperties.Components.accountUpdateTab.toString()))
			menuComponents.add(MenuComponent.builder(AccountUpdateView.class).tabName(msg.getMessage("UserHomeUI.accountUpdate")).build());
		return menuComponents;
	}

	private HorizontalLayout createImageLayout(HomeEndpointProperties homeEndpointProperties)
	{
		LoginSession theUser = InvocationContext.getCurrent().getLoginSession();
		String imageAttribute = homeEndpointProperties.getImageAttribute();
		Component image = createImage(theUser, imageAttribute);
		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", MEDIUM_MARGIN.value());
		imageLayout.getStyle().set("margin-bottom", MEDIUM_MARGIN.value());
		imageLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		imageLayout.add(image);
		return imageLayout;
	}

	private Component createImage(LoginSession theUser, String imageAttribute)
	{
		if(imageAttribute == null)
			return createDefaultImage();
		Collection<AttributeExt> attributes;
		try
		{
			attributes = attributesMan.getAttributes(
					new EntityParam(theUser.getEntityId()), "/", imageAttribute);
		} catch (EngineException e)
		{
			throw new RuntimeException(e);
		}

		if(attributes.isEmpty())
			return createDefaultImage();
		else
		{
			AttributeViewerContext context = AttributeViewerContext.builder()
					.withCustomHeight(imageSize)
					.withCustomHeightUnit(Unit.EM)
					.withCustomWidth(imageSize)
					.withCustomWidthUnit(Unit.EM)
					.withBorderRadius(50)
					.withBorderRadiusUnit(Unit.PERCENTAGE)
					.build();
			return registry.getSimpleRepresentation(attributes.iterator().next(), context);
		}
	}

	private static Image createDefaultImage()
	{
		Image tmpImage = new Image("assets/img/other/logo-square.png", "");
		tmpImage.setWidth(imageSize + "em");
		tmpImage.setHeight(imageSize + "em");
		return tmpImage;
	}

	private static Component createLoggedAsLabel(MessageSource msg)
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		String label = entity.getEntityLabel() == null ? "" : entity.getEntityLabel();
		Span loggedEntity = new Span(entity.getEntityLabel() != null ?
				msg.getMessage("MainHeader.loggedAs", label) :
				msg.getMessage("MainHeader.loggedAsWithId", entity.getEntityId()));
		loggedEntity.setId("MainHeader.loggedAs");
		return loggedEntity;
	}

	private static Component createUpmanIcon(ProjectManagementHelper projectManagementHelper)
	{
		return projectManagementHelper.getProjectManLinkIfAvailable(new HomeEndpointProperties(getCurrentWebAppContextProperties()))
				.map(HomeUiMenu::createUpmanIcon)
				.orElse(new Div());
	}

	private static Component createUpmanIcon(String url)
	{
		Icon home = VaadinIcon.FAMILY.create();
		home.addClassName(POINTER.getName());
		home.addClickListener(event -> UI.getCurrent().getPage().setLocation(url));
		return home;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
	{
		if(ComponentUtil.getData(UI.getCurrent(), HomeEndpointProperties.class) == null)
			ComponentUtil.setData(UI.getCurrent(), HomeEndpointProperties.class, homeEndpointProperties);
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		super.showRouterLayoutContent(content);
	}
}
