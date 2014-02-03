/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.serverman;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
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
 * Displays list of endpoint component 
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EndpointsComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			EndpointsComponent.class);

	private UnityMessageSource msg;
	private EndpointManagement endpointMan;
	private VerticalLayout content;
	private UnityServerConfiguration config;
	private NetworkServer networkServer;

	@Autowired
	public EndpointsComponent(UnityMessageSource msg, EndpointManagement endpointMan,
			AuthenticationManagement authMan, TranslationProfileManagement profilesMan,
			TranslationActionsRegistry tactionsRegistry, ObjectMapper jsonMapper,
			UnityServerConfiguration config, NetworkServer networkServer)
	{
		this.config = config;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.networkServer = networkServer;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("Endpoints.caption"));
		
		HorizontalLayout h = new HorizontalLayout();
		Label listCaption = new Label(msg.getMessage("Endpoints.listCaption"));
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
		refreshViewButton.setDescription(msg.getMessage("Endpoints.refreshList"));
		
		h.addComponent(listCaption);
		h.addComponent(new Label(" "));
		h.addComponent(refreshViewButton);

		addComponent(h);

		content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		addComponent(content);

		updateContent();

	}

	

	

	private void updateContent()
	{
		
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			log.error("Cannot reload configuration", e);
			ErrorPopup.showError(msg, msg
					.getMessage("DeployableComponentBase.cannotReloadConfig"),
					e);
			return;
		}

		content.removeAllComponents();

		List<EndpointDescription> endpoints = null;
		try
		{
			endpoints = endpointMan.getEndpoints();
		} catch (EngineException e)
		{
			log.error("Cannot load endpoints", e);
			ErrorPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("Endpoints.cannotLoadList"));
			return;
		}

		List<String> existing = new ArrayList<>();
		for (EndpointDescription endpointDesc : endpoints)
		{

			content.addComponent(new EndpointComponent(endpointMan, networkServer,
					endpointDesc, config, msg,
					DeployableComponentViewBase.Status.deployed.toString()));
			existing.add(endpointDesc.getId());
		}

		Set<String> endpointsList = config
				.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{
			if (!existing.contains(config.getValue(endpointKey
					+ UnityServerConfiguration.ENDPOINT_NAME)))
			{
				String name = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_NAME);
				String description = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
				EndpointDescription en = new EndpointDescription();
				en.setId(name);
				en.setDescription(description);
				content.addComponent(new EndpointComponent(endpointMan,
						networkServer, en, config, msg,
						DeployableComponentViewBase.Status.undeployed
								.toString()));
			}
		}
	}

}
