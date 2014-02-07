/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
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

	public TranslationProfileComponent(TranslationProfileManagement profilesMan,
			TranslationActionsRegistry tactionsRegistry, ObjectMapper jsonMapper,
			TranslationProfile translationProfile, UnityServerConfiguration config,
			UnityMessageSource msg, String status)
	{
		super(config, msg, status);
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

		log.info("Remove " + translationProfile.getName() + " translation profile");
		try
		{
			profilesMan.removeProfile(translationProfile.getName());
		} catch (Exception e)
		{
			log.error("Cannot remove translationProfile", e);
			ErrorPopup.showError(msg, msg.getMessage(
					"TranslationProfiles.cannotUndeploy",
					translationProfile.getName()), e);
			return;

		}

		boolean inConfig = false;
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			try
			{
				json = FileUtils.readFileToString(new File(profileFile));
			} catch (IOException e)
			{
				log.error("Cannot read json file", e);
				ErrorPopup.showError(msg,msg.getMessage("TranslationProfiles.cannotReadJsonConfig"), e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (tp.getName().equals(translationProfile.getName()))
			{
				inConfig = true;
			}
		}

		if (inConfig)
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
			return;
		
		boolean added = false;
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			try
			{
				json = FileUtils.readFileToString(new File(profileFile));
			} catch (IOException e)
			{
				log.error("Cannot read json file", e);
				ErrorPopup.showError(msg, msg.getMessage("TranslationProfiles.cannotReadJsonConfig"), e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper, tactionsRegistry);
			
			if (tp.getName().equals(translationProfile.getName()))
			{
				
				added = addTranslationProfile(tp);

			}

		}

		if (!added)
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

	private boolean addTranslationProfile(TranslationProfile tp)
	{
		log.info("Add " + tp.getName() + "translation profile");
		try
		{
			profilesMan.addProfile(tp);
		} catch (EngineException e)
		{
			log.error("Cannot add translation profile", e);
			ErrorPopup.showError(msg, msg.getMessage(
					"TranslationProfiles.cannotDeploy",
					tp.getName()), e);
			return false;
		}

		Map<String, TranslationProfile> existing;
		try
		{
			existing = profilesMan.listProfiles();
		} catch (EngineException e)
		{
			log.error("Cannot load translation profiles", e);
			ErrorPopup.showError(msg,
					msg.getMessage("TranslationProfiles.cannotLoadList"), e);
			return false;
		}

		for (TranslationProfile tr : existing.values())
		{
			if (tr.getName().equals(tp.getName()))
			{
				this.translationProfile = tr;
			}
		}

		return true;
	}

	@Override
	public void reload()
	{
		if (!super.reloadConfig())
			return;
		log.info("Reload " + translationProfile.getName() + " translation profile");
		boolean updated = false;
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			try
			{
				json = FileUtils.readFileToString(new File(profileFile));
			} catch (IOException e)
			{
				log.error("Cannot read json file", e);
				ErrorPopup.showError(msg, msg.getMessage("TranslationProfiles.cannotReadJsonConfig"), e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (tp.getName().equals(translationProfile.getName()))
			{
				updated = reloadTranslationProfile(tp);
			}

		}
		if (!updated)
		{
			new ConfirmDialog(msg, msg.getMessage(
					"TranslationProfiles.unDeployWhenRemoved",
					translationProfile.getName()), new ConfirmDialog.Callback()
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
		}
	}

	private boolean reloadTranslationProfile(TranslationProfile tp)
	{
		try
		{
			log.info("Update " + tp.getName() + " translation profile");
			profilesMan.updateProfile(tp);
		} catch (EngineException e)
		{
			log.error("Cannot update translation profile", e);
			ErrorPopup.showError(msg, msg.getMessage(
					"TranslationProfiles.cannotUpdate",
					tp.getName()), e);
			return false;
		}

		try
		{
			Map<String, TranslationProfile> existing = profilesMan.listProfiles();
			for (TranslationProfile tr : existing.values())
			{
				if (tr.getName().equals(tp.getName()))
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

}
