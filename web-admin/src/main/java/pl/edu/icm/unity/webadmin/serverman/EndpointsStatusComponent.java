/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
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
				try
				{
					reloadAuthenticators();
				} catch (EngineException | IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

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
				try
				{
					reloadTranlationProfile();
				} catch (EngineException | IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		});

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.addComponents(reloadAuthButton, reloadTransProfileButton);
		buttons.setMargin(true);
		buttons.setSpacing(true);
		addComponent(buttons);

		
		HorizontalLayout h=new HorizontalLayout();
		Label e = new Label("Endpoints:");
		h.addStyleName(Styles.bold.toString());
		Button refreshViewButton=new Button();
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
		
		h.addComponent(e);
		h.addComponent(refreshViewButton);
		
		
		addComponent(h);
		
		endpointsView = new VerticalLayout();
		endpointsView.setMargin(true);
		endpointsView.setSpacing(true);
		addComponent(endpointsView);

		updateContent();

	}

	private void reloadAuthenticators() throws EngineException, IOException
	{
		config.reloadIfChanged();
		// log.info("Loading all configured authenticators");
		Collection<AuthenticatorInstance> authenticators = authMan.getAuthenticators(null);
		Map<String, AuthenticatorInstance> existing = new HashMap<String, AuthenticatorInstance>();
		Map<String, AuthenticatorInstance> toRemove = new HashMap<String, AuthenticatorInstance>();
		
		for (AuthenticatorInstance ai : authenticators){
			existing.put(ai.getId(), ai);
			toRemove.put(ai.getId(), ai);
		}

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

			String vJsonConfiguration = vConfigFile == null ? null : FileUtils
					.readFileToString(vConfigFile);
			String rJsonConfiguration = FileUtils.readFileToString(rConfigFile);

			if (!existing.containsKey(name))
			{
				authMan.createAuthenticator(name, type, vJsonConfiguration,
						rJsonConfiguration, credential);
				// log.info(" - " + name + " [" + type + "]");
			} else
			{
				
				authMan.updateAuthenticator(name, vJsonConfiguration,
						rJsonConfiguration);
			}
			toRemove.remove(name);

		}

		for (String auth : toRemove.keySet())
		{
			authMan.removeAuthenticator(auth);
		}

	}

	private void reloadTranlationProfile() throws EngineException, IOException
	{
		config.reloadIfChanged();
		Map<String, TranslationProfile> existing = profilesMan.listProfiles();
		Map<String, TranslationProfile> toRemove = profilesMan.listProfiles();
		
		List<String> profileFiles = config
				.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		for (String profileFile : profileFiles)
		{
			String json;
			json = FileUtils.readFileToString(new File(profileFile));
			TranslationProfile tp = new TranslationProfile(json, jsonMapper,
					tactionsRegistry);

			if (existing.containsKey(tp.getName()))
			{
				profilesMan.updateProfile(tp);
				
			} else
			{
				profilesMan.addProfile(tp);
				
			}
			toRemove.remove(tp.getName());

		}
		
		for(String tp:toRemove.keySet()){
			profilesMan.removeProfile(tp);
			
		}

	}

	private void updateContent()
	{
		endpointsView.removeAllComponents();

		List<EndpointDescription> endpoints = null;
		try
		{
			endpoints = endpointMan.getEndpoints();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("EndpointsStatus.cannotLoadEndpoints"));
			return;
		}

		for (EndpointDescription endpointDesc : endpoints)
		{

			endpointsView.addComponent(new SingleEndpointComponent(endpointMan,
					endpointDesc, config, msg));

		}

	}

}
