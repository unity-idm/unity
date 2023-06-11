/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.endpoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.IdentityProviderNavigationInfoProvider;
import io.imunity.webconsole.services.base.ServicesViewBase;
import io.imunity.webconsole.spi.services.IdpServiceAdditionalAction;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * Shows IDP services list
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class IdpServicesView extends ServicesViewBase
{
	public static final String VIEW_NAME = "IdpServices";
	private IdpServiceAdditionalActionsRegistry extraActionsRegistry;

	@Autowired
	IdpServicesView(MessageSource msg, IdpServicesController controller,
			IdpServiceAdditionalActionsRegistry extraActionsRegistry)
	{
		super(msg, controller, NewIdpServiceView.VIEW_NAME, EditIdpServiceView.VIEW_NAME);
		this.extraActionsRegistry = extraActionsRegistry;
	}

	protected List<SingleActionHandler<ServiceDefinition>> getActionsHandlers()
	{
		SingleActionHandler<ServiceDefinition> editGeneral = SingleActionHandler
				.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.generalConfig"))
				.withIcon(Images.cogs.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.GENERAL)).build();

		SingleActionHandler<ServiceDefinition> editAuth = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.authenticationConfig"))
				.withIcon(Images.sign_in.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.AUTHENTICATION))
				.build();

		SingleActionHandler<ServiceDefinition> editUsers = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("IdpServicesView.usersConfig"))
				.withIcon(Images.family.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.USERS)).build();

		SingleActionHandler<ServiceDefinition> editClients = SingleActionHandler
				.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("IdpServicesView.clientsConfig"))
				.withIcon(Images.bullets.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.CLIENTS)).build();
		
		SingleActionHandler<ServiceDefinition> editPolicyAgreements= SingleActionHandler
				.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("IdpServicesView.policyAgreementsConfig"))
				.withIcon(Images.check_square.getResource())
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
	
	private void gotoExtraAction(ServiceDefinition next, String action)
	{
		NavigationHelper.goToView(AdditionalActionView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ next.getName() + "&" + CommonViewParam.action.toString() + "=" + action);
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.identityProvider.endpoints");
	}

	@Component
	public class IdpServicesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public IdpServicesNavigationInfoProvider(MessageSource msg,
				 ObjectFactory<IdpServicesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(IdentityProviderNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.identityProvider.endpoints"))
					.withIcon(Images.globe.getResource()).withPosition(10).build());

		}
	}
}
