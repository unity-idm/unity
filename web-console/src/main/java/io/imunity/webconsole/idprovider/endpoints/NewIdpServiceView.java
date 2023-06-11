/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.endpoints;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.endpoints.IdpServicesView.IdpServicesNavigationInfoProvider;
import io.imunity.webconsole.services.base.NewServiceViewBase;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Shows new IDP service editor
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewIdpServiceView extends NewServiceViewBase
{
	public static final String VIEW_NAME = "NewIdpService";

	@Autowired
	NewIdpServiceView(MessageSource msg, IdpServicesController controller)
	{
		super(msg, controller, IdpServicesView.VIEW_NAME);
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class NewServiceNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewServiceNavigationInfoProvider(ObjectFactory<NewIdpServiceView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedViewWithSubviews)
					.withParent(IdpServicesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
