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
 * Show translation profile
 * 
 * @author P. Piernik
 */
public class TranslationProfileComponent extends DeployableComponentBase
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
			UnityMessageSource msg, String status, String msgPrefix)
	{
		super(config, msg, status, msgPrefix);
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
	protected boolean undeploy()
	{
		if (super.undeploy())
		{
			log.info("Remove " + translationProfile.getName() + " translation profile");
			try
			{
				profilesMan.removeProfile(translationProfile.getName());
			} catch (Exception e)
			{
				log.error("Cannot remove translationProfile", e);
				ErrorPopup.showError(msg,
						msg.getMessage(msgPrefix + "." + "cannotUndeploy"), e);
				return false;

			}

			boolean inConfig = false;
			List<String> profileFiles = config
					.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
			for (String profileFile : profileFiles)
			{
				String json;
				try
				{
					json = FileUtils.readFileToString(new File(profileFile));
				} catch (IOException e)
				{
					log.error("Cannot read json file", e);
					ErrorPopup.showError(
							msg,
							msg.getMessage(msgPrefix
									+ ".cannotReadJsonConfig"),
							e);
					return false;
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
				setStatus(STATUS_UNDEPLOYED);
			} else
			{
				setVisible(false);
			}
		}
		return true;
	}

	@Override
	protected boolean deploy()
	{
		if (super.deploy())
		{
			log.info("Add " + translationProfile.getName() + "translation profile");
			boolean added = false;

			List<String> profileFiles = config
					.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
			for (String profileFile : profileFiles)
			{
				String json;
				try
				{
					json = FileUtils.readFileToString(new File(profileFile));
				} catch (IOException e)
				{
					log.error("Cannot read json file", e);
					ErrorPopup.showError(
							msg,
							msg.getMessage(msgPrefix
									+ ".cannotReadJsonConfig"),
							e);
					return false;
				}
				TranslationProfile tp = new TranslationProfile(json, jsonMapper,
						tactionsRegistry);

				if (tp.getName().equals(translationProfile.getName()))
				{
					try
					{
						profilesMan.addProfile(tp);
					} catch (EngineException e)
					{
						log.error("Cannot add translation profile", e);
						ErrorPopup.showError(
								msg,
								msg.getMessage(msgPrefix
										+ ".cannotDeploy"),
								e);
						return false;
					}

					Map<String, TranslationProfile> existing;
					try
					{
						existing = profilesMan.listProfiles();
					} catch (EngineException e)
					{
						log.error("Cannot load translation profiles", e);
						ErrorPopup.showError(msg, msg.getMessage(msgPrefix
								+ ".cannotLoadList"), e);
						return false;
					}

					for (TranslationProfile tr : existing.values())
					{
						if (tr.getName().equals(
								translationProfile.getName()))
						{
							this.translationProfile = tr;
						}
					}

					setStatus(STATUS_DEPLOYED);
					added = true;

				}

			}

			if (!added)
			{
				ErrorPopup.showError(
						msg,
						msg.getMessage(msgPrefix + ".cannotDeploy"),
						msg.getMessage(msgPrefix
								+ ".cannotDeployRemovedConfig"));
				setVisible(false);
				return false;

			}
		}
		return true;

	}

	@Override
	protected boolean reload()
	{
		if (super.reload())
		{
			log.info("Reload " + translationProfile.getName() + " translation profile");
			boolean updated = false;

			List<String> profileFiles = config
					.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
			for (String profileFile : profileFiles)
			{
				String json;
				try
				{
					json = FileUtils.readFileToString(new File(profileFile));
				} catch (IOException e)
				{
					log.error("Cannot read json file", e);
					ErrorPopup.showError(
							msg,
							msg.getMessage(msgPrefix
									+ ".cannotReadJsonConfig"),
							e);
					return false;
				}
				TranslationProfile tp = new TranslationProfile(json, jsonMapper,
						tactionsRegistry);

				if (tp.getName().equals(translationProfile.getName()))
				{
					try
					{
						log.info("Update " + tp.getName()+ " translation profile");
						profilesMan.updateProfile(tp);
					} catch (EngineException e)
					{
						log.error("Cannot update translation profile", e);
						ErrorPopup.showError(
								msg,
								msg.getMessage(msgPrefix
										+ ".cannotUpdate"),
								e);
						return false;
					}

					try
					{
						Map<String, TranslationProfile> existing = profilesMan
								.listProfiles();
						for (TranslationProfile tr : existing.values())
						{
							if (tr.getName().equals(
									translationProfile
											.getName()))
							{
								this.translationProfile = tr;
							}
						}
					} catch (EngineException e)
					{
						log.error("Cannot load translation profiles", e);
						ErrorPopup.showError(msg, msg.getMessage(msgPrefix
								+ ".cannotLoadList"), e);
						return false;
					}

					setStatus(STATUS_DEPLOYED);
					updated = true;

				}

			}
			if (!updated)
			{
				new ConfirmDialog(msg, msg.getMessage(msgPrefix
						+ ".unDeployWhenRemoved"),
						new ConfirmDialog.Callback()

						{

							@Override
							public void onConfirm()
							{

								undeploy();

							}
						}).show();

			}
		}
		return true;
	}

	@Override
	protected void updateContent()
	{
		content.removeAllComponents();

		if (status.equals(STATUS_DEPLOYED))
		{

			addFieldToContent(msg.getMessage(msgPrefix + ".description"),
					translationProfile.getDescription());
			addFieldToContent(msg.getMessage(msgPrefix + ".rules"), "");

			 
			
			FormLayout rulesL=new FormLayout();
		
			rulesL.setSpacing(false);
			rulesL.setMargin(false);
			
			int i=0;
			for (TranslationRule rule : translationProfile.getRules())
			{	i++;
			       
				addField(rulesL,String.valueOf(i)+":  "+msg.getMessage(msgPrefix + ".ruleCondition"),
						"<code>"+rule.getCondition().getCondition()+"</code>");
				StringBuilder params = new StringBuilder();
				for (String p : rule.getAction().getParameters())
				{
					if (params.length() > 0)
						params.append(", ");
					params.append(p);
				}
				addField(rulesL,msg.getMessage(msgPrefix + ".ruleAction"),
						"<code>"+rule.getAction().getName()+"</code>"+" "+params);

				
				content.addComponent(rulesL);
			}
			

		}
	}

}
