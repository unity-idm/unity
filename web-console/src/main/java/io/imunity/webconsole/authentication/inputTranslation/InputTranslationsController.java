/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.inputTranslation;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.server.StreamResource;

import io.imunity.webadmin.tprofile.ActionParameterComponentProvider;
import io.imunity.webadmin.tprofile.TranslationProfileEditor;
import io.imunity.webadmin.tprofile.dryrun.DryRunWizardProvider;
import io.imunity.webadmin.tprofile.wizard.ProfileWizardProvider;
import io.imunity.webconsole.WebConsoleEndpointFactory;
import io.imunity.webconsole.common.EndpointController;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

/**
 * Controller for all authentication input translation views
 * 
 * @author P.Piernik
 *
 */
@Component
class InputTranslationsController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, InputTranslationsController.class);
	
	private UnityMessageSource msg;
	private TranslationProfileManagement profileMan;
	private EndpointController endpointController;

	private InputTranslationActionsRegistry inputActionsRegistry;
	private ActionParameterComponentProvider actionComponentFactory;

	@Autowired
	InputTranslationsController(UnityMessageSource msg, TranslationProfileManagement profileMan,
			InputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProvider actionComponentFactory,
			EndpointController endpointController)
	{
		this.msg = msg;
		this.profileMan = profileMan;
		this.inputActionsRegistry = inputActionsRegistry;
		this.actionComponentFactory = actionComponentFactory;
		this.endpointController = endpointController;
	}

	TranslationProfileEditor getEditor()
			throws ControllerException
	{
		
		initActionFactory();
		try
		{
			return new TranslationProfileEditor(msg, inputActionsRegistry, ProfileType.INPUT,
					actionComponentFactory);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("InputTranslationProfilesController.getEditorError"),
					e.getMessage(), e);
		}
	}
	
	private void initActionFactory() throws ControllerException
	{
		try
		{
			actionComponentFactory.init();
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("InputTranslationProfilesController.initActionFactoryError"),
					e.getMessage(), e);
		}
		
	}

	List<TranslationProfile> getProfiles() throws ControllerException
	{
		try
		{
			return profileMan.listInputProfiles().values().stream().collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("InputTranslationProfilesController.getAllError"),
					e.getMessage(), e);
		}
	}

	TranslationProfile getProfile(String name) throws ControllerException
	{
		try
		{
			return profileMan.getInputProfile(name);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("InputTranslationProfilesController.getError", name),
					e.getMessage(), e);
		}
	}

	void removeProfile(TranslationProfile profile) throws ControllerException
	{
		try
		{
			profileMan.removeProfile(ProfileType.INPUT, profile.getName());

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InputTranslationProfilesController.removeError",
					profile.getName()), e.getMessage(), e);
		}
	}

	void addProfile(TranslationProfile profile) throws ControllerException
	{
		try
		{
			profileMan.addProfile(profile);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InputTranslationProfilesController.addError",
					profile.getName()), e.getMessage(), e);
		}
	}

	void updateProfile(TranslationProfile updated) throws ControllerException
	{
		try
		{
			profileMan.updateProfile(updated);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InputTranslationProfilesController.updateError",
					updated.getName()), e.getMessage(), e);
		}
	}

	SandboxWizardDialog getDryRunWizardDialog(SandboxAuthnRouter sandboxNotifier) throws ControllerException
	{
		DryRunWizardProvider provider = new DryRunWizardProvider(msg, getSandboxURL(), sandboxNotifier, profileMan,
				inputActionsRegistry);
		SandboxWizardDialog dialog = new SandboxWizardDialog(provider.getWizardInstance(),
				provider.getCaption());
		return dialog;
	}

	SandboxWizardDialog getWizardDialog(SandboxAuthnRouter sandboxNotifier, Runnable addCallback,  Consumer<ControllerException> errorCallback)
			throws ControllerException
	{
		
		TranslationProfileEditor editor = getEditor();

		ProfileWizardProvider wizardProvider = new ProfileWizardProvider(msg, getSandboxURL(), sandboxNotifier,
				editor, t -> addProfileSave(t, addCallback, errorCallback));
		SandboxWizardDialog dialog = new SandboxWizardDialog(wizardProvider.getWizardInstance(),
				wizardProvider.getCaption());
		return dialog;
	}
	
	private boolean addProfileSave(TranslationProfile profile, Runnable addCallback, Consumer<ControllerException> errorCallback)
	{
		try
		{
			addProfile(profile);
			addCallback.run();
		} catch (ControllerException e)
		{
			log.debug("Can nod add input translation profile", e);
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

	SimpleFileDownloader getDownloader(TranslationProfile profile) throws ControllerException
	{
		SimpleFileDownloader downloader = new SimpleFileDownloader();
		StreamResource resource = null;
		try
		{
			byte[] content = Constants.MAPPER.writeValueAsBytes(profile);
			resource = new StreamResource(() -> new ByteArrayInputStream(content),
					profile.getName() + ".json");

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("InputTranslationProfilesController.exportError",
					profile.getName()), e.getMessage(), e);
		}

		downloader.setFileDownloadResource(resource);
		return downloader;
	}

}
