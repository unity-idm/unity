/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;

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

	public EndpointComponent(EndpointManagement endpointMan, ServerManagement serverMan, NetworkServer networkServer,
			EndpointDescription endpoint, UnityServerConfiguration config,
			UnityMessageSource msg, String status)
	{
		super(config, serverMan, msg, status);
		this.endpointMan = endpointMan;
		this.endpoint = endpoint;
		this.networkServer = networkServer;
		initUI();
		setStatus(status);
	}

	@Override
	public void undeploy()
	{
		if (!super.reloadConfig())
		{
			return;
		}
		String id = endpoint.getId();
		log.info("Undeploy " + id + " endpoint");
		try
		{
			endpointMan.undeploy(id);
		} catch (Exception e)
		{
			log.error("Cannot undeploy endpoint", e);
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotUndeploy", id), e);
			return;
		}

		if (getEndpointConfig(id) != null)
		{
			setStatus(Status.undeployed.toString());
		} else
		{
			setVisible(false);
		}

	}

	@Override
	public void deploy()
	{
		if (!super.reloadConfig())
		{
			return;
		}
		String id = endpoint.getId();
		log.info("Deploy " + id + " endpoint");
		if (!deployEndpoint(id))
		{
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotDeploy",
					endpoint.getId()), msg.getMessage(
					"Endpoints.cannotDeployRemovedConfig", id));
			setVisible(false);
			return;

		}else
		{
			setStatus(Status.deployed.toString());	
		}

	}

	private boolean deployEndpoint(String id)
	{
		EndpointConfig data = getEndpointConfig(id);
		if (data == null)
		{
			return false;
		}
		
		try
		{
			this.endpoint = endpointMan.deploy(data.type, id, data.displayedName, data.address, 
					data.description, getEndpointAuth(data.authenticatorsSpec), 
					data.jsonConfiguration, data.realm);
		} catch (Exception e)
		{
			log.error("Cannot deploy endpoint", e);
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotDeploy", id), e);
			return false;
		}

		return true;
	}

	@Override
	public void reload(boolean showSuccess)
	{
		if (!super.reloadConfig())
		{
			return;
		}

		String id = endpoint.getId();
		log.info("Reload " + id + " endpoint");
		if (!reloadEndpoint(id))
		{
			new ConfirmDialog(msg, msg.getMessage("Endpoints.unDeployWhenRemoved",
					id), new ConfirmDialog.Callback()
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
			if (showSuccess)
			{
				ErrorPopup.showNotice(msg, "", msg.getMessage(
						"Endpoints.reloadSuccess", id));
			}
			
		}
		
		
		
	}
	
	private List<AuthenticatorSet> getEndpointAuth(String spec)
	{
		String[] authenticatorSets = spec.split(";");
		List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
		for (String authenticatorSet : authenticatorSets)
		{
			Set<String> endpointAuthnSet = new HashSet<String>();
			String[] authenticators = authenticatorSet.split(",");
			for (String a : authenticators)
				endpointAuthnSet.add(a.trim());
			endpointAuthn.add(new AuthenticatorSet(endpointAuthnSet));
		}
		return endpointAuthn;
	}
	
	private boolean reloadEndpoint(String id)
	{
		EndpointConfig data = getEndpointConfig(id);
		if (data == null)
		{
			return false;		
		}
		
		try
		{
			endpointMan.updateEndpoint(id, data.displayedName, data.description,
					getEndpointAuth(data.authenticatorsSpec),
					data.jsonConfiguration, data.realm);
				
			
		} catch (Exception e)
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
				if (id.equals(en.getId()))
				{
					this.endpoint = en;
				}
			}
		} catch (Exception e)
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
		I18nLabel displayedName = new I18nLabel(msg, msg.getMessage("displayedNameF"));
		displayedName.setValue(endpoint.getDisplayedName());
		addCustomFieldToContent(displayedName);
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
	
	private EndpointConfig getEndpointConfig(String name)
	{
		String endpointKey = null;
		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpoint: endpointsList)
		{

			String cname = config.getValue(endpoint + UnityServerConfiguration.ENDPOINT_NAME);
			if (name.equals(cname))
			{
				endpointKey = endpoint;
			}	
		}
		if (endpointKey == null)
		{
			return null;
		}

		EndpointConfig ret = new EndpointConfig();
		
		ret.description = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
		ret.authenticatorsSpec = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);
		ret.type = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_TYPE);
		ret.address = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_ADDRESS);
		ret.realm = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_REALM);
		ret.displayedName = config.getLocalizedString(msg, endpointKey
				+ UnityServerConfiguration.ENDPOINT_DISPLAYED_NAME);
		if (ret.displayedName.isEmpty())
			ret.displayedName.setDefaultValue(name);
		try
		{
			ret.jsonConfiguration = serverMan.loadConfigurationFile(config.getValue(endpointKey
					                           + UnityServerConfiguration.ENDPOINT_CONFIGURATION));
		} catch (Exception e)
		{
			log.error("Cannot read json file", e);
			ErrorPopup.showError(msg, msg.getMessage("Endpoints.cannotReadJsonConfig"),
					e);
			return null;
		}
		return ret;
		
	}
	
	private static class EndpointConfig
	{
		private String description;
		private String authenticatorsSpec;
		private String type;
		private String address;
		private String realm;
		private String jsonConfiguration;
		private I18nString displayedName;
	}
}
