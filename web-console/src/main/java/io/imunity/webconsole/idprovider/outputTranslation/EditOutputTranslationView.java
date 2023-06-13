/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.outputTranslation;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.outputTranslation.OutputTranslationsView.OutputTranslationsNavigationInfoProvider;
import io.imunity.webconsole.translationProfile.EditTranslationView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
public class EditOutputTranslationView extends EditTranslationView
{
	public static final String VIEW_NAME = "EditOutputTranslation";

	@Autowired
	EditOutputTranslationView(MessageSource msg, OutputTranslationsController controller)
	{
		super(msg, controller);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getViewAllName()
	{
		return OutputTranslationsView.VIEW_NAME;
	}

	@Component
	public static class EditInputTranslationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditInputTranslationNavigationInfoProvider(ObjectFactory<EditOutputTranslationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(OutputTranslationsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
