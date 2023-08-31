/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.translationProfile;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.server.StreamResource;

import io.imunity.webconsole.tprofile.ActionParameterComponentProviderV8;
import io.imunity.webconsole.tprofile.TranslationProfileEditor;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

public abstract class TranslationsControllerBase
{
	protected MessageSource msg;
	protected TranslationProfileManagement profileMan;
	
	protected TypesRegistryBase<? extends TranslationActionFactory<?>> actionsRegistry;
	private ActionParameterComponentProviderV8 actionComponentFactory;
	private ProfileType type;
	
	@Autowired
	public TranslationsControllerBase(MessageSource msg, TranslationProfileManagement profileMan,
			TypesRegistryBase<? extends TranslationActionFactory<?>> actionsRegistry,
			ActionParameterComponentProviderV8 actionComponentFactory,
			ProfileType type)
	{
		this.msg = msg;
		this.profileMan = profileMan;
		this.actionsRegistry = actionsRegistry;
		this.actionComponentFactory = actionComponentFactory;
		this.type =type;
	}
	
	protected abstract List<TranslationProfile> getProfiles() throws ControllerException;
	protected abstract TranslationProfile getProfile(String name) throws ControllerException;
	
	protected TranslationProfileEditor getEditor()
			throws ControllerException
	{
		
		initActionFactory();
		try
		{
			return new TranslationProfileEditor(msg, actionsRegistry, type,
					actionComponentFactory);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("TranslationProfilesController.getEditorError"), e);
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
					msg.getMessage("TranslationProfilesController.initActionFactoryError"), e);
		}
		
	}

	void removeProfile(TranslationProfile profile) throws ControllerException
	{
		try
		{
			profileMan.removeProfile(type, profile.getName());

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("TranslationProfilesController.removeError",
					profile.getName()), e);
		}
	}

	protected void addProfile(TranslationProfile profile) throws ControllerException
	{
		try
		{
			profileMan.addProfile(profile);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("TranslationProfilesController.addError",
					profile.getName()), e);
		}
	}

	void updateProfile(TranslationProfile updated) throws ControllerException
	{
		try
		{
			profileMan.updateProfile(updated);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("TranslationProfilesController.updateError",
					updated.getName()), e);
		}
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
			throw new ControllerException(msg.getMessage("TranslationProfilesController.exportError",
					profile.getName()), e);
		}

		downloader.setFileDownloadResource(resource);
		return downloader;
	}

}
