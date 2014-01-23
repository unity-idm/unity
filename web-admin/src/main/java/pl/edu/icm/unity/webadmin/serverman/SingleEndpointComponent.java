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
 * Show endpoint
 * 
 * @author P. Piernik
 */
@Component
public class SingleEndpointComponent extends SingleComponent
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SingleEndpointComponent.class);

	private EndpointManagement endpointMan;
	private EndpointDescription endpoint;

	public SingleEndpointComponent(EndpointManagement endpointMan,
			EndpointDescription endpoint, UnityServerConfiguration config,
			UnityMessageSource msg, String status, String msgPrefix)
	{

		super(config, msg, status, msgPrefix);
		this.endpointMan = endpointMan;
		this.endpoint = endpoint;
		initUI();
		setStatus(status);
	}

	protected boolean undeploy()
	{
		if (super.undeploy())
		{

			log.info("Undeploy " + endpoint.getId() + " endpoint");
			try
			{
				endpointMan.undeploy(endpoint.getId());
			} catch (EngineException e)
			{
				log.error("Cannot undeploy endpoint", e);
				ErrorPopup.showError(msg,
						msg.getMessage(msgPrefix + ".cannotUndeploy"), e);
				return false;

			}

			boolean inConfig = false;

			Set<String> endpointsList = config
					.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
			for (String endpointKey : endpointsList)
			{
				if (config.getValue(
						endpointKey
								+ UnityServerConfiguration.ENDPOINT_NAME)
						.equals(endpoint.getId()))
				{
					inConfig = true;
				}

			}

			if (inConfig)
			{
				setStatus(STATUS_UNDEPLOYED);
			} else
			{
				setVisible(false);
			}

		}
		return true;
	}

	protected boolean deploy()
	{
		if (super.deploy())
		{
			log.info("Deploy " + endpoint.getId() + " endpoint");
			boolean added = false;

			Set<String> endpointsList = config
					.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
			for (String endpointKey : endpointsList)
			{
				if (config.getValue(
						endpointKey
								+ UnityServerConfiguration.ENDPOINT_NAME)
						.equals(endpoint.getId()))
				{
					String description = config
							.getValue(endpointKey
									+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
					File configFile = config
							.getFileValue(endpointKey
									+ UnityServerConfiguration.ENDPOINT_CONFIGURATION,
									false);
					String authenticatorsSpec = config
							.getValue(endpointKey
									+ UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);
					String type = config.getValue(endpointKey
							+ UnityServerConfiguration.ENDPOINT_TYPE);
					String address = config
							.getValue(endpointKey
									+ UnityServerConfiguration.ENDPOINT_ADDRESS);

					String[] authenticatorSets = authenticatorsSpec.split(";");
					List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
					for (String authenticatorSet : authenticatorSets)
					{
						Set<String> endpointAuthnSet = new HashSet<String>();
						String[] authenticators = authenticatorSet
								.split(",");
						for (String a : authenticators)
							endpointAuthnSet.add(a.trim());
						endpointAuthn.add(new AuthenticatorSet(
								endpointAuthnSet));
					}

					String jsonConfiguration;
					try
					{
						jsonConfiguration = FileUtils
								.readFileToString(configFile);
					} catch (IOException e)
					{
						log.error("Cannot read json file", e);
						ErrorPopup.showError(msg, msg.getMessage(msgPrefix
								+ ".cannotReadJsonConfig"), e);
						return false;
					}
					
					try
					{
						this.endpoint = endpointMan.deploy(type,
								endpoint.getId(), address,
								description, endpointAuthn,
								jsonConfiguration);
					} catch (EngineException e)
					{
						log.error("Cannot deploy endpoint", e);
						ErrorPopup.showError(
								msg,
								msg.getMessage(msgPrefix
										+ ".cannotDeploy"),
								e);
						return false;
					}
					setStatus(STATUS_DEPLOYED);
					added = true;

				}
			}

			if (!added)
			{
				ErrorPopup.showError(
						msg,
						msg.getMessage(msgPrefix + ".cannotDeploy"),
						msg.getMessage(msgPrefix
								+ ".cannotDeployRemovedConfig"));
				setVisible(false);
				return false;

			}

		}
		return true;
	}

	protected boolean reload()
	{
		if (super.reload())
		{
			log.info("Reload " + endpoint.getId() + " endpoint");
			boolean updated = false;
			Set<String> endpointsList = config
					.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
			for (String endpointKey : endpointsList)
			{
				if (config.getValue(
						endpointKey
								+ UnityServerConfiguration.ENDPOINT_NAME)
						.equals(endpoint.getId()))
				{
					String description = config
							.getValue(endpointKey
									+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
					File configFile = config
							.getFileValue(endpointKey
									+ UnityServerConfiguration.ENDPOINT_CONFIGURATION,
									false);
					String authenticatorsSpec = config
							.getValue(endpointKey
									+ UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);

					String[] authenticatorSets = authenticatorsSpec.split(";");
					List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
					for (String authenticatorSet : authenticatorSets)
					{
						Set<String> endpointAuthnSet = new HashSet<String>();
						String[] authenticators = authenticatorSet
								.split(",");
						for (String a : authenticators)
							endpointAuthnSet.add(a.trim());
						endpointAuthn.add(new AuthenticatorSet(
								endpointAuthnSet));
					}

					String jsonConfiguration;
					try
					{
						jsonConfiguration = FileUtils
								.readFileToString(configFile);
					} catch (IOException e)
					{
						log.error("Cannot read json file", e);
						ErrorPopup.showError(msg, msg.getMessage(msgPrefix
								+ ".cannotReadJsonConfig"), e);
						return false;
					}
					log.info("Update " + endpoint.getId() + " endpoint");
					try
					{
						endpointMan.updateEndpoint(endpoint.getId(),
								description, endpointAuthn,
								jsonConfiguration);
					} catch (EngineException e)
					{
						log.error("Cannot update endpoint", e);
						ErrorPopup.showError(
								msg,
								msg.getMessage(msgPrefix
										+ ".cannotUpdate"),
								e);
						return false;
					}

					updated = true;
					try
					{
						for (EndpointDescription en : endpointMan
								.getEndpoints())
						{
							if (en.getId().equals(endpoint.getId()))
							{
								this.endpoint = en;
							}
						}
					} catch (EngineException e)
					{
						log.error("Cannot load endpoints", e);
						ErrorPopup.showError(
								msg,
								msg.getMessage("error"),
								msg.getMessage(msgPrefix
										+ ".cannotLoadList"));
					}
					setStatus(STATUS_DEPLOYED);

				}
			}

			if (!updated)
			{
				new ConfirmDialog(msg, msg.getMessage(msgPrefix
						+ ".unDeployWhenRemoved"),
						new ConfirmDialog.Callback()

						{

							@Override
							public void onConfirm()
							{

								undeploy();

							}
						}).show();

			}
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

		if (status.equals(STATUS_DEPLOYED))
		{

			addFieldToContent(msg.getMessage(msgPrefix + ".type"), endpoint.getType()
					.getName());
			addFieldToContent(msg.getMessage(msgPrefix + ".typeDescription"), endpoint
					.getType().getDescription());

			addFieldToContent(msg.getMessage(msgPrefix + ".paths"), "");

			HorizontalLayout hp = new HorizontalLayout();
			FormLayout pa = new FormLayout();
			pa.setSpacing(false);
			pa.setMargin(false);

			FormLayout pad = new FormLayout();
			pad.setSpacing(false);
			pad.setMargin(false);
			int i = 0;
			for (Map.Entry<String, String> entry : endpoint.getType().getPaths()
					.entrySet())
			{
				i++;
				addField(pa, String.valueOf(i), endpoint.getContextAddress()
						+ entry.getKey());
				addField(pad, msg.getMessage(msgPrefix + ".pathDescription"),
						entry.getValue());

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
			// Bindings
			addFieldToContent(msg.getMessage(msgPrefix + ".binding"),
					bindings.toString());

			if (endpoint.getDescription() != null
					&& endpoint.getDescription().length() > 0)
			{
				addFieldToContent(msg.getMessage(msgPrefix + ".description"),
						endpoint.getDescription());

			}
			addFieldToContent(msg.getMessage(msgPrefix + ".contextAddress"),
					endpoint.getContextAddress());

			i = 0;
			addFieldToContent(msg.getMessage(msgPrefix + ".authenticatorsSet"), "");

			FormLayout au = new FormLayout();
			au.setSpacing(false);
			au.setMargin(false);
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
				// Authenticators
				addField(au, String.valueOf(i), auth.toString());

			}
			content.addComponent(au);
		}
		
		

	}

}
