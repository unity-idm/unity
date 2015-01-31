/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

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
	private ServerManagement serverMan;
	private VerticalLayout content;
	private Map<String,AuthenticatorComponent> authenticatorComponents;

	@Autowired
	public AuthenticatorsComponent(UnityMessageSource msg, UnityServerConfiguration config,
			AuthenticationManagement authMan, ServerManagement serverMan)
	{

		this.msg = msg;
		this.config = config;
		this.authMan = authMan;
		this.serverMan = serverMan;
		this.authenticatorComponents = new TreeMap<String, AuthenticatorComponent>();
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
		refreshViewButton.addStyleName(Styles.vButtonLink.toString());
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
		reloadAllButton.addStyleName(Styles.vButtonLink.toString());
		reloadAllButton.addStyleName(Styles.toolbarButton.toString());
		reloadAllButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				reloadAuthenticators();
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
		authenticatorComponents.clear();
		
		try
		{
			serverMan.reloadConfig();
		} catch (Exception e)
		{
			setError(msg.getMessage("Configuration.cannotReloadConfig"), e);
			return;
		}

		Collection<AuthenticatorInstance> authenticators;
		try
		{
			authenticators = authMan.getAuthenticators(null);
		} catch (EngineException e)
		{
			setError(msg.getMessage("Authenticators.cannotLoadList"), e);
			return;
		}
		Set<String> existing = new HashSet<String>();

		for (AuthenticatorInstance ai : authenticators)
		{
			existing.add(ai.getId());
			authenticatorComponents.put(ai.getId(), new AuthenticatorComponent(authMan, serverMan, ai, config,
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
				authenticatorComponents.put(name ,new AuthenticatorComponent(authMan, serverMan, au, config, msg,
						DeployableComponentViewBase.Status.undeployed.toString()));
			}
		}
		
		
		for (AuthenticatorComponent auth : authenticatorComponents.values())
		{
			content.addComponent(auth);
		}
	}


	private void setError(String message, Exception error)
	{
		content.removeAllComponents();
		authenticatorComponents.clear();
		ErrorComponent ec = new ErrorComponent();
		ec.setError(message, error);
		content.addComponent(ec);
	}
	
	private void reloadAuthenticators()
	{
		updateContent();
		log.info("Reloading all authenticators");
		
		for (AuthenticatorComponent authComp : authenticatorComponents.values())
		{
			if (authComp.getStatus().equals(DeployableComponentViewBase.Status.deployed.toString()))
			{
				authComp.reload(false);
			} else if (authComp.getStatus().equals(DeployableComponentViewBase.Status.undeployed.toString()))
			{
				authComp.deploy();
			}
		}
		ErrorPopup.showNotice(msg, "", msg.getMessage(
				"Authenticators.reloaded"));
	}
}
