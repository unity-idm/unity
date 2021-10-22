/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services;

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
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * Shows services list
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class ServicesView extends ServicesViewBase
{
	public static final String VIEW_NAME = "Services";

	@Autowired
	ServicesView(MessageSource msg, ServicesController controller)
	{

		super(msg, controller, NewServiceView.VIEW_NAME, EditServiceView.VIEW_NAME);

	}

	protected List<SingleActionHandler<ServiceDefinition>> getActionsHandlers()
	{
		SingleActionHandler<ServiceDefinition> editGeneral = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.generalConfig")).withIcon(Images.cogs.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.GENERAL)).build();

		SingleActionHandler<ServiceDefinition> editAuth = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.authenticationConfig")).withIcon(Images.sign_in.getResource())
				.withHandler(r -> gotoEdit(r.iterator().next(), ServiceEditorTab.AUTHENTICATION)).build();

		return Arrays.asList(editGeneral, editAuth);

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
	public class ServicesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;

		@Autowired
		public ServicesNavigationInfoProvider(MessageSource msg, ObjectFactory<ServicesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(WebConsoleRootNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.services")).withIcon(Images.server.getResource())
					.withPosition(50).build());
		}
	}
}
