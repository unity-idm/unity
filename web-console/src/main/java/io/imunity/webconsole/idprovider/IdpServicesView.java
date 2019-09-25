/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webconsole.services.base.ServicesViewBase;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
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

	@Autowired
	IdpServicesView(UnityMessageSource msg, IdpServicesController controller)
	{
		super(msg, controller, NewIdpServiceView.VIEW_NAME, EditIdpServiceView.VIEW_NAME);
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

		return Arrays.asList(editGeneral, editAuth, editClients, editUsers);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.services");
	}

	@Component
	public class IdpServicesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public IdpServicesNavigationInfoProvider(UnityMessageSource msg,
				WebConsoleRootNavigationInfoProvider parent, ObjectFactory<IdpServicesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.idpProvider"))
					.withIcon(Images.globe.getResource()).withPosition(40).build());

		}
	}
}
