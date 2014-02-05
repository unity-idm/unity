/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;


/**
 * Displays list of translation profile component 
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TranslationProfilesComponent extends VerticalLayout
{
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			TranslationProfilesComponent.class);
	
	private UnityMessageSource msg;
	private UnityServerConfiguration config;
	private TranslationProfileManagement profilesMan;
	private TranslationActionsRegistry tactionsRegistry;
	private ObjectMapper jsonMapper;
	private VerticalLayout content;

		
	@Autowired
	public TranslationProfilesComponent(UnityMessageSource msg,
			UnityServerConfiguration config, TranslationProfileManagement profilesMan,
			TranslationActionsRegistry tactionsRegistry, ObjectMapper jsonMapper)
	{
		
		this.msg = msg;
		this.config = config;
		this.profilesMan = profilesMan;
		this.tactionsRegistry = tactionsRegistry;
		this.jsonMapper = jsonMapper;
		initUI();
	}


	private void initUI()
	{
		setCaption(msg.getMessage("TranslationProfiles.caption"));

		HorizontalLayout h = new HorizontalLayout();
		Label listCaption = new Label(msg.getMessage("TranslationProfiles.listCaption"));
		listCaption.addStyleName(Styles.bold.toString());
		h.setMargin(true);
		h.setSpacing(true);

		Button refreshViewButton = new Button();
		refreshViewButton.setIcon(Images.refresh.getResource());
		refreshViewButton.addStyleName(Reindeer.BUTTON_LINK);
		refreshViewButton.addStyleName(Styles.toolbarButton.toString());
		refreshViewButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				updateContent();

			}
		});
		refreshViewButton.setDescription(msg.getMessage("TranslationProfiles.refreshList"));

		
		Button reloadAllButton = new Button();
		reloadAllButton.setIcon(Images.transfer.getResource());
		reloadAllButton.addStyleName(Reindeer.BUTTON_LINK);
		reloadAllButton.addStyleName(Styles.toolbarButton.toString());
		reloadAllButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				reloadTranslationProfile();
				updateContent();

			}
		});
		reloadAllButton.setDescription(msg.getMessage("TranslationProfiles.reloadAll"));
		
		h.addComponent(listCaption);
		h.addComponent(new Label(" "));
		h.addComponent(refreshViewButton);
		h.addComponent(reloadAllButton);

		addComponent(h);

		content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		addComponent(content);

		updateContent();
		
		
	}
	
	
	private void updateContent()
	{
		content.removeAllComponents();
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			log.error("Cannot reload configuration", e);
			ErrorPopup.showError(msg,
					msg.getMessage("Configuration.cannotReloadConfig"), e);
			return;
		}
		Map<String, TranslationProfile> existing;
		try
		{
			existing = profilesMan.listProfiles();
		} catch (EngineException e)
		{	
			log.error("Cannot load translation profiles", e);
			ErrorPopup.showError(msg, msg.getMessage("TranslationProfiles.cannotLoadList"), e);
			return;
		}
		
		for(TranslationProfile profile:existing.values())
		{
			content.addComponent(new TranslationProfileComponent(profilesMan,
					tactionsRegistry, jsonMapper, profile, config, msg,
					DeployableComponentViewBase.Status.deployed.toString()));
		}
		
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			try
			{
				json = FileUtils.readFileToString(new File(profileFile));
			} catch (IOException e)
			{
				log.error("Cannot read json file",e);
				ErrorPopup.showError(msg,
						msg.getMessage("TranslationProfiles.cannotReadJsonConfig"),
						e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (!existing.containsKey(tp.getName()))
			{
				content.addComponent(new TranslationProfileComponent(profilesMan,
						tactionsRegistry, jsonMapper, tp, config, msg,
						DeployableComponentViewBase.Status.undeployed.toString()));

			}
		}
		
		
		
	}


	private void reloadTranslationProfile()
	{	log.info("Reloading all translation profiles");
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{	
			log.error("Cannot reload configuration",e);
			ErrorPopup.showError(msg,
					msg.getMessage("Configuration.cannotReloadConfig"), e);
			return;
		}
		Map<String, TranslationProfile> existing;
		try
		{
			existing = profilesMan.listProfiles();
		} catch (EngineException e)
		{	
			log.error("Cannot load translation profiles",e);
			ErrorPopup.showError(msg,
					msg.getMessage("TranslationProfiles.cannotLoadList"),
					e);
			return;
		}
		Map<String, TranslationProfile> toRemove = new HashMap<>(existing);
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			try
			{
				json = FileUtils.readFileToString(new File(profileFile));
			} catch (IOException e)
			{
				log.error("Cannot read json file",e);
				ErrorPopup.showError(msg,
						msg.getMessage("TranslationProfiles.cannotReadJsonConfig"),
						e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,tactionsRegistry);

			if (existing.containsKey(tp.getName()))
			{
				try
				{
					log.info("Update " + tp.getName() + " translation profile");
					profilesMan.updateProfile(tp);
				} catch (EngineException e)
				{
					log.error("Cannot update translation",e);
					ErrorPopup.showError(msg,
							msg.getMessage("TranslationProfiles.cannotUpdate", tp.getName()),
							e);
					return;
				}

			} else
			{
				try
				{	
					log.info("Add " + tp.getName() + " translation profile");
					profilesMan.addProfile(tp);
				} catch (EngineException e)
				{
					log.error("Cannot add translation profile", e);
					ErrorPopup.showError(msg,
							msg.getMessage("TranslationProfiles.cannotDeploy", tp.getName()),
							e);
					return;
				}

			}
			toRemove.remove(tp.getName());

		}

		for (String tp : toRemove.keySet())
		{
			try
			{	
				log.info("Remove " + tp + " translation profile");
				profilesMan.removeProfile(tp);
			} catch (Exception e)
			{
				log.error("Cannot remove translation profile", e);
				ErrorPopup.showError(msg,
						msg.getMessage("TranslationProfiles.cannotUndeploy", tp),
						e);
				return;
			}

		}

	}
	
	
}
