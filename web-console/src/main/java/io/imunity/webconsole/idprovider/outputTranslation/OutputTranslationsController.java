/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.outputTranslation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.common.EndpointController;
import io.imunity.webconsole.tprofile.ActionParameterComponentProvider;
import io.imunity.webconsole.translationProfile.TranslationsControllerBase;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
public class OutputTranslationsController extends TranslationsControllerBase
{
	@Autowired
	public OutputTranslationsController(MessageSource msg, TranslationProfileManagement profileMan,
			OutputTranslationActionsRegistry outputActionsRegistry,
			ActionParameterComponentProvider actionComponentFactory, EndpointController endpointController)
	{
		super(msg, profileMan, outputActionsRegistry, actionComponentFactory, ProfileType.OUTPUT);
	}

	protected List<TranslationProfile> getProfiles() throws ControllerException
	{
		try
		{
			return profileMan.listOutputProfiles().values().stream().collect(Collectors.toList());
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
			throw new ControllerException(msg.getMessage("TranslationProfilesController.getError", name),
					e);
		}
	}
}
