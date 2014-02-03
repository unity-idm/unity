/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

/**
 * Displays list of authenticator component 
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuthenticatorsComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AuthenticatorsComponent.class);

	private UnityMessageSource msg;
	private UnityServerConfiguration config;
	private AuthenticationManagement authMan;
	private VerticalLayout content;

	@Autowired
	public AuthenticatorsComponent(UnityMessageSource msg, UnityServerConfiguration config,
			AuthenticationManagement authMan)
	{

		this.msg = msg;
		this.config = config;
		this.authMan = authMan;
		initUI();
	}

	private void initUI()
	{

		setCaption(msg.getMessage("Authenticators.caption"));

		HorizontalLayout h = new HorizontalLayout();
		Label listCaption = new Label(msg.getMessage("Authenticators.listCaption"));
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
		refreshViewButton.setDescription(msg.getMessage("Authenticators.refreshList"));
		
		Button reloadAllButton = new Button();
		reloadAllButton.setIcon(Images.transfer.getResource());
		reloadAllButton.addStyleName(Reindeer.BUTTON_LINK);
		reloadAllButton.addStyleName(Styles.toolbarButton.toString());
		reloadAllButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				reloadAuthenticators();
				updateContent();

			}
		});
		reloadAllButton.setDescription(msg.getMessage("Authenticators.reloadAll"));
	
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
			ErrorPopup.showError(msg, msg.getMessage("Configuration.cannotReloadConfig"), e);
			return;
		}

		Collection<AuthenticatorInstance> authenticators;
		try
		{
			authenticators = authMan.getAuthenticators(null);
		} catch (EngineException e)
		{
			log.error("Cannot load authenticators", e);
			ErrorPopup.showError(msg,
					msg.getMessage("Authenticators.cannotLoadList"), e);
			return;
		}
		Set<String> existing = new HashSet<String>();

		for (AuthenticatorInstance ai : authenticators)
		{
			existing.add(ai.getId());
			content.addComponent(new AuthenticatorComponent(authMan, ai, config,
					msg, DeployableComponentViewBase.Status.deployed.toString()));

		}

		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			String name = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_NAME);
			if (!existing.contains(name))
			{
				AuthenticatorInstance au = new AuthenticatorInstance();
				au.setId(name);
				content.addComponent(new AuthenticatorComponent(authMan, au,config, msg,
						DeployableComponentViewBase.Status.undeployed.toString()));
			}
		}

	}

	private void reloadAuthenticators()
	{
		log.info("Reloading Authenticators");
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			log.error("Cannot reload configuration", e);
			ErrorPopup.showError(msg, msg.getMessage("Configuration.cannotReloadConfig"), e);
			return;
		}

		Collection<AuthenticatorInstance> authenticators;
		try
		{
			authenticators = authMan.getAuthenticators(null);
		} catch (EngineException e)
		{
			log.error("Cannot load authenticators", e);
			ErrorPopup.showError(msg,
					msg.getMessage("Authenticators.cannotLoadList"), e);
			return;
		}
		
		Map<String, AuthenticatorInstance> existing = new HashMap<String, AuthenticatorInstance>();

		for (AuthenticatorInstance ai : authenticators)
		{
			existing.put(ai.getId(), ai);

		}
		Map<String, AuthenticatorInstance> toRemove = new HashMap<>(existing);
		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
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
				log.error("Cannot read json file", e);
				ErrorPopup.showError(msg,
						msg.getMessage("Authenticators.cannotReadJsonConfig"), e);
				return;
			}

			if (!existing.containsKey(name))
			{
				log.info("Add " + name + " [" + type + "] authenticator");
				try
				{
					authMan.createAuthenticator(name, type, vJsonConfiguration,
							rJsonConfiguration, credential);
				} catch (EngineException e)
				{
					log.error("Cannot add authenticator", e);
					ErrorPopup.showError(msg,
							msg.getMessage("Authenticators.cannotDeploy",name),
							e);
					return;
				}

			} else
			{
				log.info("Update " + name + " [" + type + "] authenticator");
				try
				{
					authMan.updateAuthenticator(name, vJsonConfiguration,
							rJsonConfiguration);
				} catch (EngineException e)
				{
					log.error("Cannot update authenticator", e);
					ErrorPopup.showError(msg, msg
							.getMessage("Authenticators.cannotUpdate",name),
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
				log.error("Cannot remove authenticator", e);
				ErrorPopup.showError(msg,
						msg.getMessage("Authenticators.cannotUndeploy",auth), e);
				return;
			}
		}

	}
}
