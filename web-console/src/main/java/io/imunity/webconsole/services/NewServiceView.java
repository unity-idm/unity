/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.services.ServicesView.ServicesNavigationInfoProvider;
import io.imunity.webconsole.services.base.NewServiceViewBase;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Shows new service editor
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewServiceView extends NewServiceViewBase
{
	public static final String VIEW_NAME = "NewService";

	@Autowired
	NewServiceView(MessageSource msg, ServicesController controller)
	{
		super(msg, controller, ServicesView.VIEW_NAME);
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
		public NewServiceNavigationInfoProvider(ObjectFactory<NewServiceView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedViewWithSubviews)
					.withParent(ServicesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
