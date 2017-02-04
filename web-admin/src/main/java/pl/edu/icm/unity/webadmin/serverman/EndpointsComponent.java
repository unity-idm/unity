/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.serverman;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Displays list of endpoint component 
 * 
 * @author P. Piernik
 */
@PrototypeComponent
public class EndpointsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private EndpointManagement endpointMan;
	private ServerManagement serverMan;
	private VerticalLayout content;
	private UnityServerConfiguration config;
	private Map<String, EndpointComponent> endpointComponents;
	private ObjectFactory<EndpointComponent> endpointComponentFactory;

	@Autowired
	public EndpointsComponent(UnityMessageSource msg, EndpointManagement endpointMan,
			ServerManagement serverMan,
			TranslationProfileManagement profilesMan,
			ObjectMapper jsonMapper,
			UnityServerConfiguration config, 
			ObjectFactory<EndpointComponent> endpointComponentFactory)
	{
		this.config = config;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.serverMan = serverMan;
		this.endpointComponentFactory = endpointComponentFactory;
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

		List<ResolvedEndpoint> endpoints = null;
		try
		{
			endpoints = endpointMan.getEndpoints();
		} catch (EngineException e)
		{
			setError(msg.getMessage("Endpoints.cannotLoadList"), e);
			return;
		}

		Set<String> existing = new HashSet<>();
		for (ResolvedEndpoint endpointDesc : endpoints)
		{
			endpointComponents.put(endpointDesc.getName(), endpointComponentFactory.getObject().init(
					endpointDesc));
			existing.add(endpointDesc.getName());
		}

		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{	String name = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME);
			if (existing.contains(name))
				continue;
			
			EndpointComponent endpointComponent = endpointComponentFactory.getObject().init(name);
			endpointComponents.put(name, endpointComponent);
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
