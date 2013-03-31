/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;


/**
 * Vaadin UI of the authentication application. Displays configured authenticators and 
 * coordinates authentication.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AuthenticationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuthenticationUI extends UI implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	private EndpointDescription description;
	private List<Map<String, VaadinAuthentication>> authenticators;
	private LocaleChoiceComponent localeChoice;
	private UnityMessageSource msg;
	
	@Autowired
	public AuthenticationUI(LocaleChoiceComponent localeChoice, UnityMessageSource msg)
	{
		super();
		this.localeChoice = localeChoice;
		this.msg = msg;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		this.description = description;
		this.authenticators = new ArrayList<Map<String,VaadinAuthentication>>();
		for (int i=0; i<authenticators.size(); i++)
		{
			Map<String, VaadinAuthentication> map = new HashMap<String, VaadinAuthentication>();
			Map<String, BindingAuthn> origMap = authenticators.get(i);
			for (Map.Entry<String, BindingAuthn> el: origMap.entrySet())
				map.put(el.getKey(), (VaadinAuthentication)el.getValue());
			this.authenticators.add(map);
		}
	}
	
	@Override
	protected void init(final VaadinRequest request)
	{
		Component[] components = new Component[authenticators.size()];
		for (int i=0; i<components.length; i++)
			components[i] = buildAuthenticatorSetUI(authenticators.get(i), 
					description.getAuthenticatorSets().get(i));
		Component all = buildAllSetsUI(components);
		
		VerticalLayout main = new VerticalLayout();
		main.addComponent(localeChoice);
		main.addComponent(all);
		
		setContent(main);
	}
	
	private Component buildAllSetsUI(Component... setComponents)
	{
		if (setComponents.length == 1)
			return setComponents[0];
		TabSheet sheet = new TabSheet();
		for (int i=0; i<setComponents.length; i++)
			sheet.addTab(setComponents[i], msg.getMessage(
					"AuthenticationUI.authnSet", i+1));
		return sheet;
	}
	
	private Component buildAuthenticatorSetUI(Map<String, VaadinAuthentication> authenticators,
			AuthenticatorSet set)
	{
		boolean needCommonUsername = false;
		VerticalLayout mainContainer = new VerticalLayout();
		
		Label status = new Label("");
		
		HorizontalLayout authenticatorsContainer = new HorizontalLayout();		
		authenticatorsContainer.setSpacing(true);
		for (String authenticator: set.getAuthenticators())
		{
			VaadinAuthentication vaadinAuth = authenticators.get(authenticator); 
			if (vaadinAuth.needsCommonUsernameComponent())
				needCommonUsername = true;
			authenticatorsContainer.addComponent(vaadinAuth.getComponent());
		}
		
		Button authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
		authenticateButton.addClickListener(new LoginButtonListener(authenticators, set, status));
		
		mainContainer.addComponent(status);
		
		if (!needCommonUsername)
		{
			mainContainer.addComponent(authenticatorsContainer);
			mainContainer.addComponent(authenticateButton);
			return mainContainer;
		}

		UsernameComponent usernameComponent = new UsernameComponent();
		mainContainer.addComponent(usernameComponent);
		mainContainer.addComponent(authenticatorsContainer);
		for (String authenticator: set.getAuthenticators())
		{
			VaadinAuthentication vaadinAuth = authenticators.get(authenticator); 
			if (vaadinAuth.needsCommonUsernameComponent())
				vaadinAuth.setUsernameCallback(usernameComponent);
		}		
		mainContainer.addComponent(authenticateButton);
		
		return mainContainer;
	}
	
	private class LoginButtonListener implements ClickListener
	{
		private static final long serialVersionUID = 1L;
		private Map<String, VaadinAuthentication> authenticators;
		private AuthenticatorSet set;
		private Label status;
		
		public LoginButtonListener(Map<String, VaadinAuthentication> authenticators,
				AuthenticatorSet set, Label status)
		{
			this.authenticators = authenticators;
			this.set = set;
			this.status = status;
		}

		@Override
		public void buttonClick(ClickEvent event)
		{
			List<AuthenticationResult> results = new ArrayList<AuthenticationResult>();
			for (String authenticator: set.getAuthenticators())
			{
				VaadinAuthentication vaadinAuth = authenticators.get(authenticator);
				results.add(vaadinAuth.getAuthenticationResult());
			}
			
			try
			{
				AuthenticationProcessor.processResults(results);
			} catch (AuthenticationException e)
			{
				status.setValue(msg.getMessage(e.getMessage()));
			}
		}
	}
	
	private class UsernameComponent extends HorizontalLayout implements UsernameProvider
	{
		private static final long serialVersionUID = 1L;
		private TextField username;
		
		public UsernameComponent()
		{
			addComponent(new Label(msg.getMessage("AuthenticationUI.username")));
			username = new TextField();
			addComponent(username);
		}

		@Override
		public String getUsername()
		{
			return username.getValue();
		}
	}
}
