/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;

/**
 * Display endpoint fields with values
 * Allow deploy/undeploy/reload endpoint  
 * 
 * @author P. Piernik
 */
@PrototypeComponent
public class EndpointComponent extends DeployableComponentViewBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			EndpointComponent.class);

	private EndpointManagement endpointMan;
	private NetworkServer networkServer;
	
	private ResolvedEndpoint endpoint;
	private String name;

	@Autowired
	public EndpointComponent(EndpointManagement endpointMan, ServerManagement serverMan, 
			NetworkServer networkServer,
			UnityServerConfiguration config, UnityMessageSource msg)
	{
		super(config, serverMan, msg);
		this.endpointMan = endpointMan;
		this.networkServer = networkServer;
	}

	public EndpointComponent init(ResolvedEndpoint endpoint)
	{
		this.endpoint = endpoint;
		this.name = endpoint.getName();
		setStatus(Status.deployed);
		return this;
	}
	
	public EndpointComponent init(String name)
	{
		this.name = name;
		setStatus(Status.undeployed);
		return this;
	}
	
	@Override
	public void undeploy()
	{
		if (!super.reloadConfig())
		{
			return;
		}
		String id = getEndpointName();
		log.info("Undeploy " + id + " endpoint");
		try
		{
			endpointMan.undeploy(id);
		} catch (Exception e)
		{
			log.error("Cannot undeploy endpoint", e);
			NotificationPopup.showError(msg, msg.getMessage("Endpoints.cannotUndeploy", id), e);
			return;
		}

		if (getEndpointConfig(id) != null)
		{
			setStatus(Status.undeployed);
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
		String id = getEndpointName();
		log.info("Deploy " + id + " endpoint");
		if (!deployEndpoint(id))
		{
			NotificationPopup.showError(msg.getMessage("Endpoints.cannotDeploy",
					getEndpointName()), msg.getMessage(
					"Endpoints.cannotDeployRemovedConfig", id));
			setVisible(false);
			return;

		}else
		{
			setStatus(Status.deployed);	
		}

	}

	private String getEndpointName()
	{
		return name;
	}
	
	private boolean deployEndpoint(String id)
	{
		EndpointConfigExt data = getEndpointConfig(id);
		if (data == null)
		{
			return false;
		}
		
		try
		{
			this.endpoint = endpointMan.deploy(data.type, id, data.address,	data.basicConfig);
		} catch (Exception e)
		{
			log.error("Cannot deploy endpoint", e);
			NotificationPopup.showError(msg, msg.getMessage("Endpoints.cannotDeploy", id), e);
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

		String id = getEndpointName();
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
			setStatus(Status.deployed);
			if (showSuccess)
			{
				NotificationPopup.showSuccess("", msg.getMessage("Endpoints.reloadSuccess", id));
			}
			
		}
		
		
		
	}
	
	private boolean reloadEndpoint(String id)
	{
		EndpointConfigExt data = getEndpointConfig(id);
		if (data == null)
		{
			return false;		
		}
		
		try
		{
			endpointMan.updateEndpoint(id, data.basicConfig);
		} catch (Exception e)
		{
			log.error("Cannot update endpoint", e);
			NotificationPopup.showError(msg,
					msg.getMessage("Endpoints.cannotUpdate", id),
					e);
			return false;
		}

		try
		{
			for (ResolvedEndpoint en : endpointMan.getEndpoints())
			{
				if (id.equals(en.getName()))
				{
					this.endpoint = en;
				}
			}
		} catch (Exception e)
		{
			log.error("Cannot load endpoints", e);
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("Endpoints.cannotLoadList"));
			return false;
		}
		return true;
	}

	@Override
	protected void updateHeader()
	{
		updateHeader(getEndpointName());
	}

	@Override
	protected void updateContent()
	{
		content.removeAllComponents();

		if (status.equals(Status.undeployed))
			return;

		addFieldToContent(msg.getMessage("Endpoints.type"), endpoint.getType().getName());
		addFieldToContent(msg.getMessage("Endpoints.typeDescription"), endpoint.getType()
				.getDescription());
		I18nLabel displayedName = new I18nLabel(msg, msg.getMessage("displayedNameF"));
		displayedName.setValue(endpoint.getEndpoint().getConfiguration().getDisplayedName());
		addCustomFieldToContent(displayedName);
		addFieldToContent(msg.getMessage("Endpoints.paths"), "");
		
		HorizontalLayout hp = new HorizontalLayout();
		FormLayout pa = new CompactFormLayout();
		pa.setSpacing(false);
		pa.setMargin(false);

		FormLayout pad = new CompactFormLayout();
		pad.setSpacing(false);
		pad.setMargin(false);
		int i = 0;
		for (Map.Entry<String, String> entry : endpoint.getType().getPaths().entrySet())
		{
			i++;
			addField(pa, String.valueOf(i), networkServer.getAdvertisedAddress()
					+ endpoint.getEndpoint().getContextAddress() + entry.getKey());
			addField(pad, msg.getMessage("Endpoints.pathDescription"), entry.getValue());

		}
		Label space = new Label();
		space.setWidth(15, Unit.PIXELS);
		hp.addComponents(pa, space, pad);
		content.addComponent(hp);

		addFieldToContent(msg.getMessage("Endpoints.binding"), endpoint.getType().getSupportedBinding());
		
		String description = endpoint.getEndpoint().getConfiguration().getDescription();
		if (description != null && description.length() > 0)
		{
			addFieldToContent(msg.getMessage("Endpoints.description"),
					description);

		}
		addFieldToContent(msg.getMessage("Endpoints.contextAddress"),
				endpoint.getEndpoint().getContextAddress());

		addFieldToContent(msg.getMessage("Endpoints.authenticationOptions"), "");
		FormLayout au = new CompactFormLayout();
		au.setSpacing(false);
		au.setMargin(false);
		i = 0;
		for (String s : endpoint.getEndpoint().
				getConfiguration().getAuthenticationOptions())
		{
			i++;
			addField(au, String.valueOf(i), s);
		}
		content.addComponent(au);
	}
	
	private EndpointConfigExt getEndpointConfig(String name)
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

		EndpointConfigExt ret = new EndpointConfigExt();
		
		
		String description = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_DESCRIPTION);
		List<String> authn = config.getEndpointAuth(endpointKey);
		ret.type = config.getValue(endpointKey
						+ UnityServerConfiguration.ENDPOINT_TYPE);
		ret.address = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_ADDRESS);
		String realm = config.getValue(endpointKey
				+ UnityServerConfiguration.ENDPOINT_REALM);
		I18nString displayedName = config.getLocalizedString(msg, endpointKey
				+ UnityServerConfiguration.ENDPOINT_DISPLAYED_NAME);
		if (displayedName.isEmpty())
			displayedName.setDefaultValue(name);
		String jsonConfig;
		try
		{
			jsonConfig = serverMan.loadConfigurationFile(config.getValue(endpointKey
					                           + UnityServerConfiguration.ENDPOINT_CONFIGURATION));
		} catch (Exception e)
		{
			log.error("Cannot read json file", e);
			NotificationPopup.showError(msg, msg.getMessage("Endpoints.cannotReadJsonConfig"),
					e);
			return null;
		}
		
		ret.basicConfig = new EndpointConfiguration(displayedName, description, authn, jsonConfig, realm);
		return ret;
		
	}
	
	private static class EndpointConfigExt
	{
		private String type;
		private String address;
		private EndpointConfiguration basicConfig;
	}
}
