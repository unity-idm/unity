/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.released_profile;

import io.imunity.console.views.translation_profiles.TranslationsServiceBase;
import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.List;
import java.util.stream.Collectors;

@Component
class OutputTranslationsService extends TranslationsServiceBase
{
	@Autowired
	OutputTranslationsService(MessageSource msg, TranslationProfileManagement profileMan,
			OutputTranslationActionsRegistry outputActionsRegistry,
			ActionParameterComponentProvider actionComponentFactory, NotificationPresenter notificationPresenter)
	{
		super(msg, profileMan, outputActionsRegistry, actionComponentFactory, notificationPresenter, ProfileType.OUTPUT);
	}

	protected List<TranslationProfile> getProfiles() throws ControllerException
	{
		try
		{
			return profileMan.listOutputProfiles()
					.values()
					.stream()
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("TranslationProfilesController.getAllError"), e);
		}
	}

	protected TranslationProfile getProfile(String name) throws ControllerException
	{
		try
		{
			return profileMan.getOutputProfile(name);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("TranslationProfilesController.getError", name), e);
		}
	}

}
