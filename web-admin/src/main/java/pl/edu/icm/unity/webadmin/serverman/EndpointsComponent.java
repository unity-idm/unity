/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.serverman;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Displays list of endpoint component 
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EndpointsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private EndpointManagement endpointMan;
	private ServerManagement serverMan;
	private VerticalLayout content;
	private UnityServerConfiguration config;
	private NetworkServer networkServer;
	private Map<String, EndpointComponent> endpointComponents;

	@Autowired
	public EndpointsComponent(UnityMessageSource msg, EndpointManagement endpointMan,
			AuthenticationManagement authMan, ServerManagement serverMan,
			TranslationProfileManagement profilesMan,
			ObjectMapper jsonMapper,
			UnityServerConfiguration config, NetworkServer networkServer)
	{
		this.config = config;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.serverMan = serverMan;
		this.networkServer = networkServer;
		this.endpointComponents = new TreeMap<String, EndpointComponent>();
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("Endpoints.caption"));
		addStyleName(Styles.visibleScroll.toString());
		
		HorizontalLayout h = new HorizontalLayout();
		Label listCaption = new Label(msg.getMessage("Endpoints.listCaption"));
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
		refreshViewButton.setDescription(msg.getMessage("Endpoints.refreshList"));
		
		HorizontalLayout ch = new HorizontalLayout();
		ch.setSpacing(true);
		ch.addComponent(listCaption);
		ch.addComponent(new Label(" "));
		ch.addComponent(refreshViewButton);
		h.addComponent(ch);
		h.setExpandRatio(ch, 1);
		
		Label sp = new Label();
		h.addComponent(sp);
		h.setExpandRatio(sp, 2);	
		
		ErrorComponent warningC = new ErrorComponent();
		warningC.setMessage(msg.getMessage("Endpoints.reloadWarning"), Level.warning);
		addComponent(warningC);
		
		h.setSizeFull();
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
		endpointComponents.clear();
		try
		{
			serverMan.reloadConfig();
		} catch (Exception e)
		{
			setError(msg.getMessage("Configuration.cannotReloadConfig"), e);
			return;
		}

		List<EndpointDescription> endpoints = null;
		try
		{
			endpoints = endpointMan.getEndpoints();
		} catch (EngineException e)
		{
			setError(msg.getMessage("Endpoints.cannotLoadList"), e);
			return;
		}

		List<String> existing = new ArrayList<>();
		for (EndpointDescription endpointDesc : endpoints)
		{
			endpointComponents.put(endpointDesc.getId(), new EndpointComponent(
					endpointMan, serverMan, networkServer, endpointDesc, config, msg,
					DeployableComponentViewBase.Status.deployed.toString()));
			existing.add(endpointDesc.getId());
		}

		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{	String name = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME);
			if (existing.contains(name))
				continue;
			
			String description = config.getValue(endpointKey
					+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
			EndpointDescription en = new EndpointDescription();
			en.setId(name);
			en.setDescription(description);
			endpointComponents.put(en.getId(), new EndpointComponent(endpointMan, serverMan,
					networkServer, en, config, msg,
					DeployableComponentViewBase.Status.undeployed.toString()));
			
		}
		
		for (EndpointComponent endpoint : endpointComponents.values())
		{
			content.addComponent(endpoint);
		}
		
	}


	private void setError(String message, Exception error)
	{
		content.removeAllComponents();
		endpointComponents.clear();
		ErrorComponent ec = new ErrorComponent();
		ec.setError(message, error);
		content.addComponent(ec);
	}
}
