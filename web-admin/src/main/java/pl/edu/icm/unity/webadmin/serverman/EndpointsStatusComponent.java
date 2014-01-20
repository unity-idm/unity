/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
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
 * Show information about all endpoints
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EndpointsStatusComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			EndpointsStatusComponent.class);

	private UnityMessageSource msg;
	private EndpointManagement endpointMan;
	private AuthenticationManagement authMan;
	private TranslationProfileManagement profilesMan;
	private VerticalLayout endpointsView;
	private UnityServerConfiguration config;
	private TranslationActionsRegistry tactionsRegistry;
	private ObjectMapper jsonMapper;

	@Autowired
	public EndpointsStatusComponent(UnityMessageSource msg, EndpointManagement endpointMan,
			AuthenticationManagement authMan, TranslationProfileManagement profilesMan,
			TranslationActionsRegistry tactionsRegistry, ObjectMapper jsonMapper,
			UnityServerConfiguration config)
	{
		this.config = config;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.authMan = authMan;
		this.profilesMan = profilesMan;
		this.tactionsRegistry = tactionsRegistry;
		this.jsonMapper = jsonMapper;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("EndpointsStatus.caption"));
		setMargin(true);
		setSpacing(true);

		Button reloadAuthButton = new Button(
				msg.getMessage("EndpointsStatus.reloadAuthenticators"));
		reloadAuthButton.setIcon(Images.refresh.getResource());
		reloadAuthButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{

				reloadAuthenticators();

			}
		});

		Button reloadTransProfileButton = new Button(
				msg.getMessage("EndpointsStatus.reloadTranslationProfiles"));
		reloadTransProfileButton.setIcon(Images.refresh.getResource());
		reloadTransProfileButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{

				reloadTranslationProfile();

			}

		});

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.addComponents(reloadAuthButton, reloadTransProfileButton);
		buttons.setMargin(true);
		buttons.setSpacing(true);
		addComponent(buttons);

		HorizontalLayout h = new HorizontalLayout();
		Label e = new Label(msg.getMessage("EndpointsStatus.listCaption"));
		e.addStyleName(Styles.bold.toString());
		
		
		Button refreshViewButton = new Button();
		refreshViewButton.setIcon(Images.refresh.getResource());
		refreshViewButton.addStyleName(Reindeer.BUTTON_LINK);
		refreshViewButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				updateContent();

			}
		});
		refreshViewButton.setDescription(msg.getMessage("EndpointsStatus.refreshEndpointsList"));
		
		h.addComponent(e);
		h.addComponent(new Label(" "));
		h.addComponent(refreshViewButton);

		addComponent(h);

		endpointsView = new VerticalLayout();
		endpointsView.setMargin(true);
		endpointsView.setSpacing(true);
		addComponent(endpointsView);

		updateContent();

	}

	private void reloadAuthenticators()
	{
		log.info("Reloading Authenticators");
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{	log.error("Cannot reload configuration",e);
			ErrorPopup.showError(msg,
					msg.getMessage("EndpointsStatus.cannotReloadConfig"), e);
			return;
		}
		
		Collection<AuthenticatorInstance> authenticators;
		try
		{	
			authenticators = authMan.getAuthenticators(null);
		} catch (EngineException e)
		{       
			log.error("Cannot load authenticators",e);
			ErrorPopup.showError(msg,
					msg.getMessage("EndpointsStatus.cannotGetAuthenticators"),
					e);
			return;
		}
		Map<String, AuthenticatorInstance> existing = new HashMap<String, AuthenticatorInstance>();

		for (AuthenticatorInstance ai : authenticators)
		{
			existing.put(ai.getId(), ai);

		}
		Map<String, AuthenticatorInstance> toRemove = new HashMap<>(existing);

		Set<String> authenticatorsList = config
				.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			String name = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_NAME);
			String type = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_TYPE);
			File vConfigFile = config
					.getFileValue(authenticatorKey
							+ UnityServerConfiguration.AUTHENTICATOR_VERIFICATOR_CONFIG,
							false);
			File rConfigFile = config.getFileValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_RETRIEVAL_CONFIG,
					false);
			String credential = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_CREDENTIAL);

			String vJsonConfiguration = null;
			String rJsonConfiguration = null;
			try
			{
				vJsonConfiguration = vConfigFile == null ? null : FileUtils
						.readFileToString(vConfigFile);
				rJsonConfiguration = FileUtils.readFileToString(rConfigFile);
			} catch (IOException e)
			{	
				log.error("Cannot read json file",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("EndpointsStatus.cannotReadJsonConfig"),
						e);
				return;
			}

			if (!existing.containsKey(name))
			{
				log.info("Add " + name + " [" + type + "]");
				try
				{
					authMan.createAuthenticator(name, type, vJsonConfiguration,
							rJsonConfiguration, credential);
				} catch (EngineException e)
				{
					log.error("Cannot add authenticator",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.cannotAddAuthenticator"),
							e);
					return;
				}

			} else
			{
				log.info("Update " + name + " [" + type + "]");
				try
				{
					authMan.updateAuthenticator(name, vJsonConfiguration,
							rJsonConfiguration);
				} catch (EngineException e)
				{
					log.error("Cannot update authenticator",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.cannotUpdateAuthenticator"),
							e);
					return;
				}

			}
			toRemove.remove(name);

		}

		for (String auth : toRemove.keySet())
		{
			try
			{
				log.info("Remove " + auth + " authenticator");
				authMan.removeAuthenticator(auth);
			} catch (Exception e)
			{
				log.error("Cannot remove authenticator",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("EndpointsStatus.cannotRemoveAuthenticator"),
						e);
				return;
			}
		}

	}

	private void reloadTranslationProfile()
	{	log.info("Reloading translation profile");
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{	log.error("Cannot reload configuration",e);
			ErrorPopup.showError(msg,
					msg.getMessage("EndpointsStatus.cannotReloadConfig"), e);
			return;
		}
		Map<String, TranslationProfile> existing;
		try
		{
			existing = profilesMan.listProfiles();
		} catch (EngineException e)
		{	
			log.error("Cannot load translation profiles",e);
			ErrorPopup.showError(
					msg,
					msg.getMessage("EndpointsStatus.cannotGetTranslationProfiles"),
					e);
			return;
		}
		Map<String, TranslationProfile> toRemove = new HashMap<>(existing);

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
				log.error("Cannot read json file",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("EndpointsStatus.cannotReadJsonConfig"),
						e);
				return;
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (existing.containsKey(tp.getName()))
			{
				try
				{
					log.info("Update "+tp.getName());
					profilesMan.updateProfile(tp);
				} catch (EngineException e)
				{
					log.error("Cannot update translation",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.cannotUpdateTranslationProfile"),
							e);
					return;
				}

			} else
			{
				try
				{	log.info("Add "+tp.getName());
					profilesMan.addProfile(tp);
				} catch (EngineException e)
				{
					log.error("Cannot add translation profile",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("EndpointsStatus.cannotAddTranslationProfile"),
							e);
					return;
				}

			}
			toRemove.remove(tp.getName());

		}

		for (String tp : toRemove.keySet())
		{
			try
			{	log.info("Remove "+tp);
				profilesMan.removeProfile(tp);
			} catch (Exception e)
			{
				log.error("Cannot remove translation profile",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("EndpointsStatus.cannotRemoveTranslationProfile"),
						e);
				return;
			}

		}

	}

	private void updateContent()
	{
		
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{	log.error("Cannot reload configuration",e);
			ErrorPopup.showError(msg,msg.getMessage("EndpointsStatus.cannotReloadConfig") , e);
			return;    
		}
		
		
		endpointsView.removeAllComponents();

		List<EndpointDescription> endpoints = null;
		try
		{
			endpoints = endpointMan.getEndpoints();
		} catch (EngineException e)
		{	
			log.error("Cannot load endpoints",e);
			ErrorPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("EndpointsStatus.cannotLoadEndpoints"));
			return;
		}

		List<String> existing=new ArrayList<>();
		for (EndpointDescription endpointDesc : endpoints)
		{

			endpointsView.addComponent(new SingleEndpointComponent(endpointMan,
					endpointDesc, config, msg,SingleEndpointComponent.STATUS_DEPLOYED));
			existing.add(endpointDesc.getId());
		}
		
		
		
		
		
		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey: endpointsList)
		{
			if(!existing.contains(config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_NAME)))
			{
				String name = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_NAME);
				String description = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_DESCRIPTION);		
				
				
				EndpointDescription en=new EndpointDescription();
				en.setId(name);
				en.setDescription(description);
				endpointsView.addComponent(new SingleEndpointComponent(endpointMan,
						en, config, msg,SingleEndpointComponent.STATUS_UNDEPLOYED));
				
				
				
				
	
			}
		}
		
		
		
		
		
		
		

	}

}
