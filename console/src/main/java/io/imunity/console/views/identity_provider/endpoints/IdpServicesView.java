/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.identity_provider.released_profile.endpoints.spi.IdpServiceAdditionalAction;
import io.imunity.console.views.services.base.ServicesViewBase;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;


/**
 * Shows IDP services list
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Breadcrumb(key = "WebConsoleMenu.identityProvider.endpoints", parent = "WebConsoleMenu.identityProvider")
@Route(value = "/idpServices", layout = ConsoleMenu.class)
public class IdpServicesView extends ServicesViewBase
{
	private IdpServiceAdditionalActionsRegistry extraActionsRegistry;

	@Autowired
	IdpServicesView(MessageSource msg, IdpServicesController controller, NotificationPresenter notificationPresenter,
			IdpServiceAdditionalActionsRegistry extraActionsRegistry)
	{
		super(msg, notificationPresenter, controller, NewIdpServiceView.class, EditIdpServiceView.class);
		this.extraActionsRegistry = extraActionsRegistry;
		initUI();
	}

	protected List<SingleActionHandler<ServiceDefinition>> getActionsHandlers()
	{
		SingleActionHandler<ServiceDefinition> editGeneral = SingleActionHandler
				.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.generalConfig"))
				.withIcon(VaadinIcon.COGS)
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.GENERAL)).build();

		SingleActionHandler<ServiceDefinition> editAuth = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.authenticationConfig"))
				.withIcon(VaadinIcon.SIGN_IN)
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.AUTHENTICATION))
				.build();

		SingleActionHandler<ServiceDefinition> editUsers = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("IdpServicesView.usersConfig"))
				.withIcon(VaadinIcon.FAMILY)
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.USERS)).build();

		SingleActionHandler<ServiceDefinition> editClients = SingleActionHandler
				.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("IdpServicesView.clientsConfig"))
				.withIcon(VaadinIcon.BULLETS)
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.CLIENTS)).build();
		
		SingleActionHandler<ServiceDefinition> editPolicyAgreements= SingleActionHandler
				.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("IdpServicesView.policyAgreementsConfig"))
				.withIcon(VaadinIcon.CHECK_SQUARE)
				.hideIfInactive()
				.withDisabledPredicate(s -> !s.getBinding().equals(VaadinAuthentication.NAME.toString()))
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.POLICY_AGREEMENTS)).build();

		return Stream.concat(getAdditionalActionsHandlers().stream(),
				Arrays.asList(editPolicyAgreements, editGeneral, editClients, editUsers, editAuth).stream())
				.collect(Collectors.toList());

	}

	private List<SingleActionHandler<ServiceDefinition>> getAdditionalActionsHandlers()
	{
		List<SingleActionHandler<ServiceDefinition>> additionalActions = new ArrayList<>();

		for (IdpServiceAdditionalAction action : extraActionsRegistry.getAll())
		{
			SingleActionHandler<ServiceDefinition> actionHandler = SingleActionHandler
					.builder(ServiceDefinition.class)
					.withCaption(action.getActionRepresentation().caption)
					.withIcon(action.getActionRepresentation().icon)
					.withHandler(r -> gotoExtraAction(r.iterator().next(), action.getName()))
					.withDisabledPredicate(r -> !r.getType().equals(action.getSupportedServiceType()))
					.hideIfInactive().build();
			additionalActions.add(actionHandler);
		}
		return additionalActions;
	}
	
	//TODO
	private void gotoExtraAction(ServiceDefinition next, String action)
	{
//		NavigationHelper.goToView(AdditionalActionView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
//				+ next.getName() + "&" + CommonViewParam.action.toString() + "=" + action);
	}
}
