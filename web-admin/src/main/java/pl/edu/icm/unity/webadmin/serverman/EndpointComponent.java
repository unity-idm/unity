/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

/**
 * Display endpoint fields with values
 * Allow deploy/undeploy/reload endpoint  
 * 
 * @author P. Piernik
 */
@Component
public class EndpointComponent extends DeployableComponentViewBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			EndpointComponent.class);

	private EndpointManagement endpointMan;
	private EndpointDescription endpoint;
	private NetworkServer networkServer;

	public EndpointComponent(EndpointManagement endpointMan, NetworkServer networkServer,
			EndpointDescription endpoint, UnityServerConfiguration config,
			UnityMessageSource msg, String status)
	{
		super(config, msg, status);
		this.endpointMan = endpointMan;
		this.endpoint = endpoint;
		this.networkServer = networkServer;
		initUI();
		setStatus(status);
	}

	public void undeploy()
	{
		if (!super.reloadConfig())
			return;

		log.info("Undeploy " + endpoint.getId() + " endpoint");
		try
		{
			endpointMan.undeploy(endpoint.getId());
		} catch (EngineException e)
		{
			log.error("Cannot undeploy endpoint", e);
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotUndeploy",
					endpoint.getId()), e);
			return;

		}

		boolean inConfig = false;
		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{
			if (config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME)
					.equals(endpoint.getId()))
			{
				inConfig = true;
			}

		}

		if (inConfig)
		{
			setStatus(Status.undeployed.toString());
		} else
		{
			setVisible(false);
		}

	}

	public void deploy()
	{
		if (!super.reloadConfig())
			return;
		
		log.info("Deploy " + endpoint.getId() + " endpoint");
		boolean added = false;
		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{
			if (config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME)
					.equals(endpoint.getId()))
			{
				added = deployEndpoint(endpointKey, endpoint.getId());
	
			}
		}

		if (!added)
		{
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotDeploy",
					endpoint.getId()), msg.getMessage(
					"Endpoints.cannotDeployRemovedConfig", endpoint.getId()));
			setVisible(false);
			return;

		}else
		{
			setStatus(Status.deployed.toString());	
		}

	}

	private boolean deployEndpoint(String endpointKey, String id)
	{
		String description = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
		File configFile = config.getFileValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_CONFIGURATION, false);
		String authenticatorsSpec = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);
		String type = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_TYPE);
		String address = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_ADDRESS);

		String[] authenticatorSets = authenticatorsSpec.split(";");
		List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
		for (String authenticatorSet : authenticatorSets)
		{
			Set<String> endpointAuthnSet = new HashSet<String>();
			String[] authenticators = authenticatorSet.split(",");
			for (String a : authenticators)
				endpointAuthnSet.add(a.trim());
			endpointAuthn.add(new AuthenticatorSet(endpointAuthnSet));
		}

		String jsonConfiguration;
		try
		{
			jsonConfiguration = FileUtils.readFileToString(configFile);
		} catch (IOException e)
		{
			log.error("Cannot read json file", e);
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotReadJsonConfig"),
					e);
			return false;
		}

		try
		{
			this.endpoint = endpointMan.deploy(type, id, address, description,
					endpointAuthn, jsonConfiguration);
		} catch (EngineException e)
		{
			log.error("Cannot deploy endpoint", e);
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotDeploy", id), e);
			return false;
		}

		return true;
	}

	public void reload()
	{
		if (!super.reloadConfig())
			return;
		
		log.info("Reload " + endpoint.getId() + " endpoint");
		boolean updated = false;
		Set<String> endpointsList = config
				.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{
			if (config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME)
					.equals(endpoint.getId()))
			{
				updated = reloadEndpoint(endpointKey, endpoint.getId());
				

			}
		}

		if (!updated)
		{
			new ConfirmDialog(msg, msg.getMessage("Endpoints.unDeployWhenRemoved",
					endpoint.getId()), new ConfirmDialog.Callback()

			{

				@Override
				public void onConfirm()
				{

					undeploy();

				}
			}).show();

		}else
		{
			setStatus(Status.deployed.toString());
		}
	
	}

	private boolean reloadEndpoint(String endpointKey, String id)
	{
		String description = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
		File configFile = config.getFileValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_CONFIGURATION, false);
		String authenticatorsSpec = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);

		String[] authenticatorSets = authenticatorsSpec.split(";");
		List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
		for (String authenticatorSet : authenticatorSets)
		{
			Set<String> endpointAuthnSet = new HashSet<String>();
			String[] authenticators = authenticatorSet.split(",");
			for (String a : authenticators)
				endpointAuthnSet.add(a.trim());
			endpointAuthn.add(new AuthenticatorSet(endpointAuthnSet));
		}

		String jsonConfiguration;
		try
		{
			jsonConfiguration = FileUtils.readFileToString(configFile);
		} catch (IOException e)
		{
			log.error("Cannot read json file", e);
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotReadJsonConfig"),
					e);
			return false;
		}
		
		try
		{
			endpointMan.updateEndpoint(endpoint.getId(), description, endpointAuthn,
					jsonConfiguration);
		} catch (EngineException e)
		{
			log.error("Cannot update endpoint", e);
			ErrorPopup.showError(msg,
					msg.getMessage("Endpoints.cannotUpdate", id),
					e);
			return false;
		}

		try
		{
			for (EndpointDescription en : endpointMan.getEndpoints())
			{
				if (en.getId().equals(id))
				{
					this.endpoint = en;
				}
			}
		} catch (EngineException e)
		{
			log.error("Cannot load endpoints", e);
			ErrorPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("Endpoints.cannotLoadList"));
			return false;
		}
		return true;
	}

	protected void updateHeader()
	{
		updateHeader(endpoint.getId());
	}

	protected void updateContent()
	{
		content.removeAllComponents();

		if (status.equals(Status.undeployed.toString()))
			return;

		addFieldToContent(msg.getMessage("Endpoints.type"), endpoint.getType().getName());
		addFieldToContent(msg.getMessage("Endpoints.typeDescription"), endpoint.getType()
				.getDescription());
		addFieldToContent(msg.getMessage("Endpoints.paths"), "");

		HorizontalLayout hp = new HorizontalLayout();
		FormLayout pa = new FormLayout();
		pa.setSpacing(false);
		pa.setMargin(false);

		FormLayout pad = new FormLayout();
		pad.setSpacing(false);
		pad.setMargin(false);
		int i = 0;
		for (Map.Entry<String, String> entry : endpoint.getType().getPaths().entrySet())
		{
			i++;
			addField(pa, String.valueOf(i), networkServer.getAdvertisedAddress()
					+ endpoint.getContextAddress() + entry.getKey());
			addField(pad, msg.getMessage("Endpoints.pathDescription"), entry.getValue());

		}
		Label space = new Label();
		space.setWidth(15, Unit.PIXELS);
		hp.addComponents(pa, space, pad);
		content.addComponent(hp);

		StringBuilder bindings = new StringBuilder();
		for (String s : endpoint.getType().getSupportedBindings())
		{
			if (bindings.length() > 0)
				bindings.append(",");
			bindings.append(s);
		}
		addFieldToContent(msg.getMessage("Endpoints.binding"), bindings.toString());
		
		if (endpoint.getDescription() != null && endpoint.getDescription().length() > 0)
		{
			addFieldToContent(msg.getMessage("Endpoints.description"),
					endpoint.getDescription());

		}
		addFieldToContent(msg.getMessage("Endpoints.contextAddress"),
				endpoint.getContextAddress());

		addFieldToContent(msg.getMessage("Endpoints.authenticatorsSet"), "");
		FormLayout au = new FormLayout();
		au.setSpacing(false);
		au.setMargin(false);
		i = 0;
		for (AuthenticatorSet s : endpoint.getAuthenticatorSets())
		{
			i++;
			StringBuilder auth = new StringBuilder();
			for (String a : s.getAuthenticators())
			{
				if (auth.length() > 0)
					auth.append(",");
				auth.append(a);
			}
			addField(au, String.valueOf(i), auth.toString());
		}
		content.addComponent(au);
	}
}
