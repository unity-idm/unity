/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.utils.ProjectManagementHelper;
import io.imunity.vaadin.elements.MenuComponent;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.layout.UnityAppLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppContextProperties;
import static java.util.stream.Collectors.toList;

public class HomeUiMenu extends UnityAppLayout
{
	private final AttributesManagement attributesMan;
	private final AttributeHandlerRegistry registry;

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
						MenuComponent.builder(TrustedDeviceView.class).tabName(msg.getMessage("UserHomeUI.trustedDevice"))
								.build(),
						MenuComponent.builder(TrustedApplicationsView.class).tabName(msg.getMessage("UserHomeUI.trustedApplications"))
								.build(),
						MenuComponent.builder(AccountUpdateView.class).tabName(msg.getMessage("UserHomeUI.accountUpdate"))
								.build()
						)
						.collect(toList()), standardWebLogoutHandler, createUpmanIcon(projectManagementHelper)
		);
		this.attributesMan = attributesMan;
		this.registry = registry;

		HomeEndpointProperties homeEndpointProperties = new HomeEndpointProperties(getCurrentWebAppContextProperties());
		ComponentUtil.setData(UI.getCurrent(), HomeEndpointProperties.class, homeEndpointProperties);

		HorizontalLayout imageLayout = createImageLayout(homeEndpointProperties);

		super.initView();

		addToLeftContainerAsFirst(imageLayout);
	}

	private HorizontalLayout createImageLayout(HomeEndpointProperties homeEndpointProperties)
	{
		LoginSession theUser = InvocationContext.getCurrent().getLoginSession();
		String imageAttribute = homeEndpointProperties.getImageAttribute();
		Collection<AttributeExt> attributes;
		try
		{
			attributes = attributesMan.getAttributes(
					new EntityParam(theUser.getEntityId()), "/", imageAttribute);
		} catch (EngineException e)
		{
			throw new RuntimeException(e);
		}

		Component image;
		if(attributes.isEmpty())
		{
			Image tmpImage = new Image("../unitygw/img/other/logo-hand.png", "");
			tmpImage.setWidth("7em");
			tmpImage.setHeight("7em");
			image = tmpImage;
		}
		else
		{
			AttributeViewerContext context = AttributeViewerContext.builder()
					.withCustomHeight(7)
					.withCustomHeightUnit(Unit.EM)
					.withCustomWidth(7)
					.withCustomWidthUnit(Unit.EM)
					.withBorderRadius(50)
					.withBorderRadiusUnit(Unit.PERCENTAGE)
					.build();
			image = registry.getSimpleRepresentation(attributes.iterator().next(), context);
		}
		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", "1.5em");
		imageLayout.getStyle().set("margin-bottom", "1.5em");
		imageLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		imageLayout.add(image);
		return imageLayout;
	}

	private static List<Component> createUpmanIcon(ProjectManagementHelper projectManagementHelper)
	{
		return projectManagementHelper.getProjectManLinkIfAvailable(new HomeEndpointProperties(getCurrentWebAppContextProperties()))
				.map(HomeUiMenu::createUpmanIcon)
				.stream().collect(toList());
	}

	private static Component createUpmanIcon(String url)
	{
		Icon home = VaadinIcon.FAMILY.create();
		home.getStyle().set("cursor", "pointer");
		home.addClickListener(event -> UI.getCurrent().getPage().setLocation(url));
		return home;
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		super.showRouterLayoutContent(content);
	}
}
