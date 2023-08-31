/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.inputTranslation;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleEndpointFactory;
import io.imunity.webconsole.common.EndpointController;
import io.imunity.webconsole.tprofile.ActionParameterComponentProviderV8;
import io.imunity.webconsole.tprofile.TranslationProfileEditor;
import io.imunity.webconsole.translationProfile.TranslationsControllerBase;
import io.imunity.webconsole.translationProfile.dryrun.DryRunWizardProvider;
import io.imunity.webconsole.translationProfile.wizard.ProfileWizardProvider;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

@Component
public class InputTranslationsController extends TranslationsControllerBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InputTranslationsController.class);
	private EndpointController endpointController;

	@Autowired
	public InputTranslationsController(MessageSource msg, TranslationProfileManagement profileMan,
									   InputTranslationActionsRegistry inputActionsRegistry,
									   ActionParameterComponentProviderV8 actionComponentFactory, EndpointController endpointController)
	{
		super(msg, profileMan, inputActionsRegistry, actionComponentFactory, ProfileType.INPUT);
		this.endpointController = endpointController;
	}

	protected List<TranslationProfile> getProfiles() throws ControllerException
	{
		try
		{
			return profileMan.listInputProfiles().values().stream().collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InputTranslationProfilesController.getAllError"),
					e);
		}
	}

	protected TranslationProfile getProfile(String name) throws ControllerException
	{
		try
		{
			return profileMan.getInputProfile(name);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("InputTranslationProfilesController.getError", name), e);
		}
	}

	SandboxWizardDialog getDryRunWizardDialog(SandboxAuthnRouter sandboxNotifier) throws ControllerException
	{
		DryRunWizardProvider provider = new DryRunWizardProvider(msg, getSandboxURL(), sandboxNotifier,
				profileMan, (InputTranslationActionsRegistry) actionsRegistry);
		SandboxWizardDialog dialog = new SandboxWizardDialog(provider.getWizardInstance(),
				provider.getCaption());
		return dialog;
	}

	SandboxWizardDialog getWizardDialog(SandboxAuthnRouter sandboxNotifier, Runnable addCallback,
			Consumer<ControllerException> errorCallback) throws ControllerException
	{

		TranslationProfileEditor editor = getEditor();

		ProfileWizardProvider wizardProvider = new ProfileWizardProvider(msg, getSandboxURL(), sandboxNotifier,
				editor, t -> addProfileSave(t, addCallback, errorCallback));
		SandboxWizardDialog dialog = new SandboxWizardDialog(wizardProvider.getWizardInstance(),
				wizardProvider.getCaption());
		return dialog;
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
			log.info("Can not add input translation profile", e);
			errorCallback.accept(e);
			return false;
		}
		return true;
	}

	private String getSandboxURL() throws ControllerException
	{
		List<ResolvedEndpoint> endpointList = endpointController.getEndpoints();
		for (ResolvedEndpoint endpoint : endpointList)
		{
			if (endpoint.getType().getName().equals(WebConsoleEndpointFactory.NAME))
			{
				return endpoint.getEndpoint().getContextAddress()
						+ VaadinEndpoint.SANDBOX_PATH_TRANSLATION;
			}
		}
		return null;
	}
}
