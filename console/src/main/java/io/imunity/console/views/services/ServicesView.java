/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.services.base.ServicesViewBase;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;


/**
 * Shows services list
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Breadcrumb(key = "WebConsoleMenu.services")
@Route(value = "/services", layout = ConsoleMenu.class)
public class ServicesView extends ServicesViewBase
{

	@Autowired
	ServicesView(MessageSource msg, ServicesController controller, NotificationPresenter notificationPresenter)
	{

		super(msg, notificationPresenter,  controller, NewServiceView.class, EditServiceView.class);
		initUI();
	}

	protected List<SingleActionHandler<ServiceDefinition>> getActionsHandlers()
	{
		SingleActionHandler<ServiceDefinition> editGeneral = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.generalConfig")).withIcon(VaadinIcon.COGS)
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.GENERAL)).build();

		SingleActionHandler<ServiceDefinition> editAuth = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.authenticationConfig")).withIcon(VaadinIcon.SIGN_IN)
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.AUTHENTICATION)).build();

		return Arrays.asList(editGeneral, editAuth);
	}
}
