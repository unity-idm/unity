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

import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.TopHeaderLight;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;



/**
 * Vaadin UI of the authentication application. Displays configured authenticators and 
 * coordinates authentication.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AuthenticationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
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
			components[i] = new AuthenticatorSetComponent(authenticators.get(i), 
					description.getAuthenticatorSets().get(i), msg);
		Component all = buildAllSetsUI(components);
		
		VerticalLayout main = new VerticalLayout();
		
		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_LEFT);

		Label vSpacer = new Label("");
		//vSpacer.setHeight(10, Unit.PERCENTAGE);
		main.addComponent(vSpacer);
		main.setExpandRatio(vSpacer, 1.0f);
		
		main.addComponent(all);
		main.setComponentAlignment(all, Alignment.TOP_CENTER);
		main.setExpandRatio(all, 5.0f);
		main.setSpacing(true);
		main.setMargin(true);
		main.setSizeFull();

		VerticalLayout topLevel = new VerticalLayout();
		TopHeaderLight header = new TopHeaderLight(msg.getMessage("AuthenticationUI.login",
				description.getId()), msg);
		topLevel.addComponents(header, main);
		topLevel.setSizeFull();
		topLevel.setExpandRatio(main, 1.0f);
		
		setContent(topLevel);
		setSizeFull();
	}
	
	private Component buildAllSetsUI(final Component... setComponents)
	{
		if (setComponents.length == 1)
		{
			if (setComponents[0] instanceof ActivationListener)
				((ActivationListener)setComponents[0]).stateChanged(true);
			return setComponents[0];
		}
		HorizontalLayout all = new HorizontalLayout();
		final Panel currentAuthnSet = new Panel();
		AuthenticatorSetChangedListener setChangeListener = new AuthenticatorSetChangedListener()
		{
			private ActivationListener last = null;
			
			@Override
			public void setWasChanged(int i)
			{
				Component c = setComponents[i];
				currentAuthnSet.setContent(c);
				if (last != null)
					last.stateChanged(false);
				if (c instanceof ActivationListener)
				{
					last = (ActivationListener)c;
					last.stateChanged(true);
				}
			}
		};
		AuthenticatorSetSelectComponent setSelection = new AuthenticatorSetSelectComponent(msg, 
				setChangeListener, description, authenticators);

		currentAuthnSet.setContent(setComponents[0]);
		
		all.addComponent(setSelection);
		all.setComponentAlignment(setSelection, Alignment.TOP_CENTER);
		all.addComponent(currentAuthnSet);
		all.setComponentAlignment(currentAuthnSet, Alignment.TOP_CENTER);
		all.setSpacing(true);
		all.setSizeFull();
		return all;
	}
}
