/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Display authenticator fields with values
 * Allow deploy/undeploy/reload authenticator
 * 
 * @author P. Piernik
 */
@PrototypeComponent
public class AuthenticatorComponent extends DeployableComponentViewBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AuthenticatorComponent.class);

	private AuthenticatorInfo authenticator;
	private AuthenticatorManagement authMan;

	@Autowired
	public AuthenticatorComponent(AuthenticatorManagement authMan, ServerManagement serverMan,
			UnityServerConfiguration config, UnityMessageSource msg)
	{
		super(config, serverMan , msg);
		this.authMan = authMan;
	}

	public AuthenticatorComponent init(AuthenticatorInfo authenticator, Status status)
	{
		this.authenticator = authenticator;
		setStatus(status);
		return this;
	}

	@Override
	protected void updateHeader()
	{
		super.updateHeader(authenticator.getId());
	}

	@Override
	protected void updateContent()
	{
		content.removeAllComponents();
		
		if (status.equals(Status.undeployed))
		{
			return;
		}
		
		addFieldToContent(msg.getMessage("Authenticators.type"), 
				authenticator.getTypeDescription().getVerificationMethod());
		addFieldToContent(msg.getMessage("Authenticators.verificationMethodDescription"),
				authenticator.getTypeDescription()
						.getVerificationMethodDescription());
		addFieldToContent(msg.getMessage("Authenticators.supportedBinding"), 
				authenticator.getSupportedBindings().toString());
		Optional<String> cr = authenticator.getLocalCredentialName();
		if (cr.isPresent())
			addFieldToContent(msg.getMessage("Authenticators.localCredential"), cr.get());
		
		addConfigPanel(msg.getMessage("Authenticators.configuration"), authenticator.getConfiguration());
	}
	
	
	private void addConfigPanel(String capion, String val)
	{
		if (val != null && !val.isEmpty())
		{
			addFieldToContent(capion, "");
			Panel p = new SafePanel();
			Label valL = new Label(val, ContentMode.PREFORMATTED);
			valL.setSizeUndefined();
			p.setContent(valL);
			content.addComponent(p);
		}
	}

	@Override
	public void undeploy()
	{
		if (!super.reloadConfig())
		{
			return;
		}
		String id = authenticator.getId();
		log.info("Remove " + id + " authenticator");
		try
		{
			authMan.removeAuthenticator(authenticator.getId());
		} catch (Exception e)
		{
			log.error("Cannot remove authenticator", e);
			NotificationPopup.showError(msg, msg.getMessage("Authenticators.cannotUndeploy", id), e);
			return;

		}

		if (getAuthenticatorConfig(id) !=null)
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
		String id = authenticator.getId();
		log.info("Add " + id + "authenticator");
		if (!addAuthenticator(id))
		{
			NotificationPopup.showError(
					msg.getMessage("Authenticators.cannotDeploy", id),
					msg.getMessage("Authenticators.cannotDeployRemovedConfig", id));
			setVisible(false);
			return;

		}else
		{
			setStatus(Status.deployed);
		}
	}
	
	private boolean addAuthenticator(String name)
	{	
		AuthenticatorConfig data = getAuthenticatorConfig(name);
		if (data == null)
			return false;
		try
		{
			this.authenticator = authMan.createAuthenticator(name, data.type, data.config, data.credential);
		} catch (Exception e)
		{
			log.error("Cannot add authenticator", e);
			NotificationPopup.showError(msg, msg.getMessage("Authenticators.cannotDeploy",
					name), e);
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
		String id = authenticator.getId();
		log.info("Reload " + id + " authenticator");
		if (!reloadAuthenticator(id))
		{
			new ConfirmDialog(msg, msg.getMessage("Authenticators.unDeployWhenRemoved",
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
				NotificationPopup.showSuccess("", msg.getMessage(
						"Authenticators.reloadSuccess", id));
			}
		}
	}
	
	private boolean reloadAuthenticator(String name)
	{
		AuthenticatorConfig data = getAuthenticatorConfig(name);
		if (data == null)
		{
			return false;
		}
			
		try
		{
			authMan.updateAuthenticator(name, data.config, data.credential);
		} catch (Exception e)
		{
			log.error("Cannot update authenticator", e);
			NotificationPopup.showError(msg, msg.getMessage("Authenticators.cannotDeploy", name), e);
			return false;
		}

		try
		{
			for (AuthenticatorInfo au : authMan.getAuthenticators(null))
			{
				if (au.getId().equals(name))
				{
					this.authenticator = au;
					break;
				}
			}
		} catch (Exception e)
		{
			log.error("Cannot load authenticators", e);
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("Authenticators.cannotLoadList"));
			return false;
		}
		return true;
	}
	
	private AuthenticatorConfig getAuthenticatorConfig(String name)
	{
		String authenticatorKey = null;
		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticator: authenticatorsList)
		{

			String cname = config.getValue(authenticator + UnityServerConfiguration.AUTHENTICATOR_NAME);
			if (name.equals(cname))
				authenticatorKey = authenticator;
		}
		
		if (authenticatorKey == null)
			return null;

		String type = config.getValue(authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_TYPE);
		String credential = config.getValue(authenticatorKey 
				+ UnityServerConfiguration.AUTHENTICATOR_CREDENTIAL);
		String externalizedConfig;
		try
		{
			String vConfigFile = config.getValue(authenticatorKey
							+ UnityServerConfiguration.AUTHENTICATOR_VERIFICATOR_CONFIG);
			externalizedConfig = vConfigFile == null ? null : serverMan.loadConfigurationFile(vConfigFile);
		} catch (Exception e)
		{
			log.error("Cannot read config file", e);
			NotificationPopup.showError(msg, msg.getMessage("Authenticators.cannotReadConfig"), e);
			return null;
		}	
		return new AuthenticatorConfig(type, externalizedConfig, credential);
	}
	
	private static class AuthenticatorConfig
	{
		private String type;
		private String config;
		private String credential;

		AuthenticatorConfig(String type, String config, String credential)
		{
			this.type = type;
			this.config = config;
			this.credential = credential;
		}
	}
}
