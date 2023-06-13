/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.inputTranslation;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.inputTranslation.InputTranslationsView.InputTranslationsNavigationInfoProvider;
import io.imunity.webconsole.translationProfile.EditTranslationView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
class EditInputTranslationView extends EditTranslationView
{
	public static final String VIEW_NAME = "EditInputTranslation";

	@Autowired
	EditInputTranslationView(MessageSource msg, InputTranslationsController controller)
	{
		super(msg, controller);
	}

	@Override
	public String getViewAllName()
	{
		return InputTranslationsView.VIEW_NAME;
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class EditInputTranslationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditInputTranslationNavigationInfoProvider(
				ObjectFactory<EditInputTranslationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(InputTranslationsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
