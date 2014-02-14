/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.ui.FormLayout;

/**
 * Display translation profile fields with values
 * Allow deploy/undeploy/reload translation profile
 * 
 * @author P. Piernik
 */ 
@Component
class TranslationProfileComponent extends DeployableComponentViewBase
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			TranslationProfileComponent.class);

	private TranslationProfileManagement profilesMan;
	private TranslationActionsRegistry tactionsRegistry;
	private ObjectMapper jsonMapper;
	private TranslationProfile translationProfile;

	public TranslationProfileComponent(TranslationProfileManagement profilesMan, ServerManagement serverMan,
			TranslationActionsRegistry tactionsRegistry, ObjectMapper jsonMapper,
			TranslationProfile translationProfile, UnityServerConfiguration config,
			UnityMessageSource msg, String status)
	{
		super(config, serverMan, msg, status);
		this.tactionsRegistry = tactionsRegistry;
		this.jsonMapper = jsonMapper;
		this.profilesMan = profilesMan;
		this.translationProfile = translationProfile;
		setStatus(status);
	}

	@Override
	protected void updateHeader()
	{
		super.updateHeader(translationProfile.getName());

	}

	@Override
	public void undeploy()
	{
		if (!super.reloadConfig())
			return;

		String name = translationProfile.getName();
		log.info("Remove " + name + " translation profile");
		try
		{
			profilesMan.removeProfile(name);
		} catch (Exception e)
		{
			log.error("Cannot remove translationProfile", e);
			ErrorPopup.showError(msg, msg.getMessage(
					"TranslationProfiles.cannotUndeploy", name), e);
			return;
		}

		if (getTranslationProfile(name) != null)
		{
			setStatus(Status.undeployed.toString());
		} else
		{
			setVisible(false);
		}
	}

	@Override
	public void deploy()
	{
		if (!super.reloadConfig())
		{
			return;
		}		
		if (!addTranslationProfile(translationProfile.getName()))
		{
			ErrorPopup.showError(msg, msg.getMessage(
					"TranslationProfiles.cannotDeploy",
					translationProfile.getName()), msg.getMessage(
					"TranslationProfiles.cannotDeployRemovedConfig",
					translationProfile.getName()));
			setVisible(false);
			return;
		} else
		{
			setStatus(Status.deployed.toString());
		}	

	}

	private boolean addTranslationProfile(String name)
	{
		log.info("Add " + name + "translation profile");
		TranslationProfile tp = getTranslationProfile(name);
		if(tp == null)
		{
			return false;
		}
		try
		{
			profilesMan.addProfile(tp);
		} catch (EngineException e)
		{
			log.error("Cannot add translation profile", e);
			ErrorPopup.showError(msg, msg.getMessage(
					"TranslationProfiles.cannotDeploy", name), e);
			return false;
		}

		Map<String, TranslationProfile> existing;
		try
		{
			existing = profilesMan.listProfiles();
		} catch (EngineException e)
		{
			log.error("Cannot load translation profiles", e);
			ErrorPopup.showError(msg, msg.getMessage("TranslationProfiles.cannotLoadList"), e);
			return false;
		}

		for (TranslationProfile tr : existing.values())
		{
			if (name.equals(tr.getName()))
			{
				this.translationProfile = tr;
			}
		}

		return true;
	}

	@Override
	public void reload(boolean showSuccess)
	{
		if (!super.reloadConfig())
		{
			return;
		}
		String name = translationProfile.getName();
		log.info("Reload " + name + " translation profile");
		if (!reloadTranslationProfile(name))
		{
			new ConfirmDialog(msg, msg.getMessage(
					"TranslationProfiles.unDeployWhenRemoved",
					name), new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					undeploy();
				}
			}).show();

		} else
		{
			setStatus(Status.deployed.toString());
			if (showSuccess)
			{
				ErrorPopup.showNotice(msg, "", msg.getMessage(
						"TranslationProfiles.reloadSuccess", name));
			}
		}
	}

	private boolean reloadTranslationProfile(String name)
	{
		TranslationProfile tp = getTranslationProfile(name);
		if (tp == null)
		{
			return false;
		}
		try
		{
			log.info("Update " + name + " translation profile");
			profilesMan.updateProfile(tp);
		} catch (EngineException e)
		{
			log.error("Cannot update translation profile", e);
			ErrorPopup.showError(msg, msg.getMessage(
					"TranslationProfiles.cannotUpdate", name), e);
			return false;
		}

		try
		{
			Map<String, TranslationProfile> existing = profilesMan.listProfiles();
			for (TranslationProfile tr : existing.values())
			{
				if (name.equals(tr.getName()))
				{
					this.translationProfile = tr;
				}
			}
		} catch (EngineException e)
		{
			log.error("Cannot load translation profiles", e);
			ErrorPopup.showError(msg,
					msg.getMessage("TranslationProfiles.cannotLoadList"), e);
			return false;
		}
		return true;
	}

	@Override
	protected void updateContent()
	{
		content.removeAllComponents();

		if (status.equals(Status.deployed.toString()))
		{
			addFieldToContent(msg.getMessage("TranslationProfiles.description"),
					translationProfile.getDescription());
			addFieldToContent(msg.getMessage("TranslationProfiles.rules"), "");
			
			FormLayout rulesL=new FormLayout();
			rulesL.setSpacing(false);
			rulesL.setMargin(false);
			
			int i=0;
			for (TranslationRule rule : translationProfile.getRules())
			{	i++;
			       
				addField(rulesL, String.valueOf(i) + ":  " + msg.getMessage("TranslationProfiles.ruleCondition"),
						"<code>" + rule.getCondition().getCondition() + "</code>");
				StringBuilder params = new StringBuilder();
				for (String p : rule.getAction().getParameters())
				{
					if (params.length() > 0)
						params.append(", ");
					params.append(p);
				}
				addField(rulesL, msg.getMessage("TranslationProfiles.ruleAction"),
						"<code>" + rule.getAction().getName() + "</code>" + " " + params);
		
				content.addComponent(rulesL);
			}
		}
	}
	
	private TranslationProfile getTranslationProfile(String name)
	{
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			try
			{
				json = serverMan.loadConfigurationFile(profileFile);
			} catch (EngineException e)
			{
				log.error("Cannot read json file", e);
				ErrorPopup.showError(msg, msg.getMessage("TranslationProfiles.cannotReadJsonConfig"), e);
				return null;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (tp.getName().equals(name))
			{
				return tp;
			}

		}
		return null;	
	}
}
