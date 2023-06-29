/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
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
import io.imunity.vaadin.endpoint.common.layout.UnityAppLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppContextProperties;
import static java.util.stream.Collectors.toList;

@CssImport(value = "./styles/components/vaadin-accordion-panel.css", themeFor = "vaadin-accordion-panel")
public class HomeUiMenu extends UnityAppLayout implements BeforeEnterObserver
{
	private final static int imageSize = 7;
	private final AttributesManagement attributesMan;
	private final AttributeHandlerRegistry registry;
	private final HomeEndpointProperties homeEndpointProperties;

	@Autowired
	public HomeUiMenu(VaddinWebLogoutHandler standardWebLogoutHandler, MessageSource msg,
					  ProjectManagementHelper projectManagementHelper, AttributesManagement attributesMan,
					  AttributeHandlerRegistry registry)
	{
		super(Stream.of(
						MenuComponent.builder(ProfileView.class).tabName(msg.getMessage("UserHomeUI.profile"))
								.build(),
						MenuComponent.builder(SignInView.class).tabName(msg.getMessage("UserHomeUI.signIn"))
								.build(),
						MenuComponent.builder(TrustedApplicationsView.class).tabName(msg.getMessage("UserHomeUI.trustedApplications"))
								.build(),
						MenuComponent.builder(TrustedDeviceView.class).tabName(msg.getMessage("UserHomeUI.trustedDevices"))
								.build(),
						MenuComponent.builder(AccountUpdateView.class).tabName(msg.getMessage("UserHomeUI.accountUpdate"))
								.build()
						)
						.collect(toList()), standardWebLogoutHandler, List.of(createLoggedAsLabel(msg), createUpmanIcon(projectManagementHelper))
		);
		this.attributesMan = attributesMan;
		this.registry = registry;
		this.homeEndpointProperties = new HomeEndpointProperties(getCurrentWebAppContextProperties());
		ComponentUtil.setData(UI.getCurrent(), HomeEndpointProperties.class, homeEndpointProperties);

		HorizontalLayout imageLayout = createImageLayout(homeEndpointProperties);

		super.initView();

		addToLeftContainerAsFirst(imageLayout);
	}

	private HorizontalLayout createImageLayout(HomeEndpointProperties homeEndpointProperties)
	{
		LoginSession theUser = InvocationContext.getCurrent().getLoginSession();
		String imageAttribute = homeEndpointProperties.getImageAttribute();
		Component image = createImage(theUser, imageAttribute);
		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", "var(--medium-margin)");
		imageLayout.getStyle().set("margin-bottom", "var(--medium-margin)");
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
		Image tmpImage = new Image("../unitygw/img/other/logo-hand.png", "");
		tmpImage.setWidth(imageSize + "em");
		tmpImage.setHeight(imageSize + "em");
		return tmpImage;
	}

	private static Component createLoggedAsLabel(MessageSource msg)
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		String label = entity.getEntityLabel() == null ? "" : entity.getEntityLabel();
		Label loggedEntity = new Label(entity.getEntityLabel() != null ?
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
		home.getStyle().set("cursor", "pointer");
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
