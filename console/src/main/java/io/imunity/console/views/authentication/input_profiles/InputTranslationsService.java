/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.console.views.authentication.input_profiles.wizard.ProfileWizardProvider;
import io.imunity.vaadin.auth.sandbox.SandboxWizardDialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
class InputTranslationsService extends TranslationsServiceBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InputTranslationsService.class);
	private final ProfileWizardProvider profileWizardProvider;

	@Autowired
	InputTranslationsService(MessageSource msg, TranslationProfileManagement profileMan,
			InputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProvider actionComponentFactory, NotificationPresenter notificationPresenter,
			ProfileWizardProvider profileWizardProvider)
	{
		super(msg, profileMan, inputActionsRegistry, actionComponentFactory, notificationPresenter, ProfileType.INPUT);
		this.profileWizardProvider = profileWizardProvider;
	}

	protected List<TranslationProfile> getProfiles() throws ControllerException
	{
		try
		{
			return profileMan.listInputProfiles()
					.values()
					.stream()
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InputTranslationProfilesController.getAllError"), e);
		}
	}

	protected TranslationProfile getProfile(String name) throws ControllerException
	{
		try
		{
			return profileMan.getInputProfile(name);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("TranslationProfilesController.getError", name), e);
		}
	}

	SandboxWizardDialog getWizardDialog(Runnable addCallback, Consumer<ControllerException> errorCallback)
			throws ControllerException
	{
		SandboxWizardDialog sandboxWizardDialog = new SandboxWizardDialog();
		sandboxWizardDialog
				.add(profileWizardProvider.getWizard(getEditor(), sandboxWizardDialog::close, t ->
				{
					addProfileSave(t, addCallback, errorCallback);
					sandboxWizardDialog.close();
				}));
		return sandboxWizardDialog;
	}

	private boolean addProfileSave(TranslationProfile profile, Runnable addCallback,
			Consumer<ControllerException> errorCallback)
	{
		try
		{
			addProfile(profile);
			addCallback.run();
		} catch (ControllerException e)
		{
			log.error("Can not add input translation profile", e);
			errorCallback.accept(e);
			return false;
		}
		return true;
	}

}
