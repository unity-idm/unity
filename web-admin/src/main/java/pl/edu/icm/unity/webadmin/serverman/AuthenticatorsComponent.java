/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webadmin.serverman.DeployableComponentViewBase.Status;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Displays list of authenticator component 
 * 
 * @author P. Piernik
 */
@PrototypeComponent
public class AuthenticatorsComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AuthenticatorsComponent.class);

	private UnityMessageSource msg;
	private UnityServerConfiguration config;
	private AuthenticatorManagement authMan;
	private ServerManagement serverMan;
	private VerticalLayout content;
	private Map<String,AuthenticatorComponent> authenticatorComponents;

	private ObjectFactory<AuthenticatorComponent> authenticatorComponentFactory;

	@Autowired
	public AuthenticatorsComponent(UnityMessageSource msg, UnityServerConfiguration config,
			AuthenticatorManagement authMan, ServerManagement serverMan,
			ObjectFactory<AuthenticatorComponent> authenticatorComponentFactory)
	{

		this.msg = msg;
		this.config = config;
		this.authMan = authMan;
		this.serverMan = serverMan;
		this.authenticatorComponentFactory = authenticatorComponentFactory;
		this.authenticatorComponents = new TreeMap<>();
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("Authenticators.caption"));
		addStyleName(Styles.visibleScroll.toString());

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
		reloadAllButton.setIcon(Images.reload.getResource());
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

		Collection<AuthenticatorInfo> authenticators;
		try
		{
			authenticators = authMan.getAuthenticators(null);
		} catch (EngineException e)
		{
			setError(msg.getMessage("Authenticators.cannotLoadList"), e);
			return;
		}
		Set<String> existing = new HashSet<>();

		for (AuthenticatorInfo ai : authenticators)
		{
			existing.add(ai.getId());
			authenticatorComponents.put(ai.getId(), 
					authenticatorComponentFactory.getObject().init(ai, Status.deployed));
		}

		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			String name = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_NAME);
			if (!existing.contains(name))
			{
				AuthenticatorInfo au = new AuthenticatorInfo(name, null, null, Optional.empty(), null);
				authenticatorComponents.put(name, 
						authenticatorComponentFactory.getObject().init(au, Status.undeployed));
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
			if (authComp.getStatus().equals(Status.deployed))
			{
				authComp.reload(false);
			} else if (authComp.getStatus().equals(Status.undeployed))
			{
				authComp.deploy();
			}
		}
		NotificationPopup.showSuccess("", msg.getMessage("Authenticators.reloaded"));
	}
}
