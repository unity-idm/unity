/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.ErrorComponent;
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
	private ServerManagement serverMan;
	private TranslationActionsRegistry tactionsRegistry;
	private ObjectMapper jsonMapper;
	private VerticalLayout content;
	private Map<String,TranslationProfileComponent> translationProfileComponents;

		
	@Autowired
	public TranslationProfilesComponent(UnityMessageSource msg,
			UnityServerConfiguration config, TranslationProfileManagement profilesMan,
			ServerManagement serverMan, TranslationActionsRegistry tactionsRegistry,
			ObjectMapper jsonMapper)
	{
		this.msg = msg;
		this.config = config;
		this.profilesMan = profilesMan;
		this.serverMan = serverMan;
		this.tactionsRegistry = tactionsRegistry;
		this.jsonMapper = jsonMapper;
		this.translationProfileComponents = new TreeMap<String, TranslationProfileComponent>();
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
		translationProfileComponents.clear();
		try
		{
			serverMan.reloadConfig();
		} catch (Exception e)
		{
			setError(msg.getMessage("Configuration.cannotReloadConfig"), e);
			return;
		}
		Map<String, TranslationProfile> existing;
		try
		{
			existing = profilesMan.listProfiles();
		} catch (EngineException e)
		{
			setError(msg.getMessage("TranslationProfiles.cannotLoadList"), e);
			return;
		}
		
		for (TranslationProfile profile : existing.values())
		{
			translationProfileComponents.put(
					profile.getName(),
					new TranslationProfileComponent(profilesMan, serverMan,
							tactionsRegistry, jsonMapper, profile,
							config, msg,
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
				setError(msg.getMessage("TranslationProfiles.cannotReadJsonConfig"), e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (!existing.containsKey(tp.getName()))
			{
				translationProfileComponents.put(
						tp.getName(),
						new TranslationProfileComponent(profilesMan, serverMan,
								tactionsRegistry, jsonMapper, tp,
								config, msg,
								DeployableComponentViewBase.Status.undeployed.toString()));
			}
		}
		
		
		for (TranslationProfileComponent tp : translationProfileComponents.values())
		{
			content.addComponent(tp);
		}
		
		
	}

	private void setError(String message, Exception error)
	{
		content.removeAllComponents();
		translationProfileComponents.clear();
		ErrorComponent ec = new ErrorComponent();
		ec.setError(message, error);
		content.addComponent(ec);
	}

	private void reloadTranslationProfile()
	{	log.info("Reloading all translation profiles");
		updateContent();

		for (TranslationProfileComponent tpComp : translationProfileComponents.values())
		{
			if (tpComp.getStatus().equals(
					DeployableComponentViewBase.Status.deployed.toString()))
			{
				tpComp.reload(false);
			} else if (tpComp.getStatus().equals(
					DeployableComponentViewBase.Status.undeployed.toString()))
			{
				tpComp.deploy();
			}

		}
	}
	
	
}
