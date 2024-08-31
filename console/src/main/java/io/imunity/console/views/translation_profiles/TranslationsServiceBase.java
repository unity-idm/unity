/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.translation_profiles;

import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.TooltipFactory;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

import java.util.List;

public abstract class TranslationsServiceBase
{
	protected final MessageSource msg;
	protected final TranslationProfileManagement profileMan;
	protected final NotificationPresenter notificationPresenter;
	
	protected final TypesRegistryBase<? extends TranslationActionFactory<?>> actionsRegistry;
	private final ActionParameterComponentProvider actionComponentFactory;
	private final ProfileType type;
	
	@Autowired
	public TranslationsServiceBase(MessageSource msg, TranslationProfileManagement profileMan,
			TypesRegistryBase<? extends TranslationActionFactory<?>> actionsRegistry,
			ActionParameterComponentProvider actionComponentFactory, NotificationPresenter notificationPresenter,
			ProfileType type)
	{
		this.msg = msg;
		this.profileMan = profileMan;
		this.actionsRegistry = actionsRegistry;
		this.actionComponentFactory = actionComponentFactory;
		this.type =type;
		this.notificationPresenter = notificationPresenter;
	}
	
	protected abstract List<TranslationProfile> getProfiles() throws ControllerException;
	protected abstract TranslationProfile getProfile(String name) throws ControllerException;
	
	protected TranslationProfileEditor getEditor()
			throws ControllerException
	{
		
		initActionFactory();
		try
		{
			
			return new TranslationProfileEditor(msg, actionsRegistry, type, actionComponentFactory,
					notificationPresenter, new TooltipFactory(), type.equals(ProfileType.INPUT) ? profileMan.listInputProfiles()
							.keySet()
							: profileMan.listOutputProfiles()
									.keySet());
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
}
