/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

/**
 * Display authenticator fields with values
 * Allow deploy/undeploy/reload authenticator
 * 
 * @author P. Piernik
 */
@Component
public class AuthenticatorComponent extends DeployableComponentViewBase
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AuthenticatorComponent.class);

	private AuthenticatorInstance authenticator;
	private AuthenticationManagement authMan;

	public AuthenticatorComponent(AuthenticationManagement authMan, ServerManagement serverMan,
			AuthenticatorInstance authenticator, UnityServerConfiguration config,
			UnityMessageSource msg, String status)
	{
		super(config, serverMan , msg, status);
		this.authenticator = authenticator;
		this.authMan = authMan;
		setStatus(status);
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
		
		if (status.equals(Status.undeployed.toString()))
		{
			return;
		}
		
		addFieldToContent(msg.getMessage("Authenticators.type"), authenticator
				.getTypeDescription().getId());
		addFieldToContent(msg.getMessage("Authenticators.verificationMethod"),
				authenticator.getTypeDescription().getVerificationMethod());
		addFieldToContent(msg.getMessage("Authenticators.verificationMethodDescription"),
				authenticator.getTypeDescription()
						.getVerificationMethodDescription());
		addFieldToContent(msg.getMessage("Authenticators.retrievalMethod"), authenticator
				.getTypeDescription().getRetrievalMethod());
		addFieldToContent(msg.getMessage("Authenticators.retrievalMethodDescription"),
				msg.getMessage(authenticator.getTypeDescription()
						.getRetrievalMethodDescription()));
		addFieldToContent(msg.getMessage("Authenticators.supportedBinding"), authenticator
				.getTypeDescription().getSupportedBinding());
		String cr = authenticator.getLocalCredentialName();
		if (cr != null && !cr.isEmpty())
		{
			addFieldToContent(msg.getMessage("Authenticators.localCredential"), cr);
		}
		
		addConfigPanel(msg.getMessage("Authenticators.verificatorJsonConfiguration"), authenticator.getVerificatorJsonConfiguration());
		addConfigPanel(msg.getMessage("Authenticators.retrievalJsonConfiguration"), authenticator.getRetrievalJsonConfiguration());
	}
	
	
	private void addConfigPanel(String capion ,String val)
	{
		if (val != null && !val.isEmpty())
		{
			addFieldToContent(capion, "");
			Panel p = new Panel();
			p.setWidth(500, Unit.PIXELS);
			p.setHeight(150, Unit.PIXELS);
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

		log.info("Remove " + authenticator.getId() + " authenticator");
		try
		{
			authMan.removeAuthenticator(authenticator.getId());
		} catch (Exception e)
		{
			log.error("Cannot remove authenticator", e);
			ErrorPopup.showError(msg, msg.getMessage("Authenticators.cannotUndeploy",
					authenticator.getId()), e);
			return;

		}

		boolean inConfig = false;
		Set<String> authenticatorsList = config
				.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			if (config.getValue(authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_NAME)
					.equals(authenticator.getId()))
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

	@Override
	public void deploy()
	{
		if (!super.reloadConfig())
		{
			return;
		}
		
		log.info("Add " + authenticator.getId() + "authenticator");
		boolean added = false;
		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey: authenticatorsList)
		{

			String name = config.getValue(authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_NAME);
			if (authenticator.getId().equals(name))
			{
				added = addAuthenticator(authenticatorKey, name);
			}	
			
		}

		if (!added)
		{
			ErrorPopup.showError(msg, msg.getMessage("Authenticators.cannotDeploy",
					authenticator.getId()), msg.getMessage(
					"Authenticators.cannotDeployRemovedConfig",
					authenticator.getId()));
			setVisible(false);
			return;

		}else
		{
			setStatus(Status.deployed.toString());
		}
	}
	
	private boolean addAuthenticator(String authenticatorKey, String name)
	{	
		Map<String, String> data = loadAuthenticatorConfig(authenticatorKey, name);
		if (data == null)
		{
			return false;
		}
		try
		{
			this.authenticator = authMan.createAuthenticator(name, data.get("type"), data.get("vJsonConfiguration"),
					data.get("rJsonConfiguration"), data.get("credential"));
		} catch (EngineException e)
		{
			log.error("Cannot add authenticator", e);
			ErrorPopup.showError(msg, msg.getMessage("Authenticators.cannotDeploy",
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
		
		log.info("Reload " + authenticator.getId() + " authenticator");
		boolean updated = false;
		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			String name = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_NAME);
			if (authenticator.getId().equals(name))
			{
				updated = reloadAuthenticator(authenticatorKey, name);
			}
			
			
		}

		if (!updated)
		{
			new ConfirmDialog(msg, msg.getMessage("Authenticators.unDeployWhenRemoved",
					authenticator.getId()), new ConfirmDialog.Callback()
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
						"Authenticators.reloadSuccess", authenticator.getId()));
			}
		}
	}
	
	private boolean reloadAuthenticator(String authenticatorKey, String name)
	{
		Map<String, String> data = loadAuthenticatorConfig(authenticatorKey, name);
		if (data == null)
		{
			return false;
		}
			
		try
		{
			authMan.updateAuthenticator(name, data.get("vJsonConfiguration"),
					data.get("rJsonConfiguration"), data.get("credential"));
		} catch (EngineException e)
		{
			log.error("Cannot update authenticator", e);
			ErrorPopup.showError(msg, msg.getMessage(
					"Authenticators.cannotDeploy",
					name), e);
			return false;
		}

		try
		{
			for (AuthenticatorInstance au : authMan.getAuthenticators(null))
			{
				if (au.getId().equals(name))
				{
					this.authenticator = au;
				}
			}
		} catch (EngineException e)
		{
			log.error("Cannot load authenticators", e);
			ErrorPopup.showError(msg,msg.getMessage("error"),
					msg.getMessage("Authenticators.cannotLoadList"));
			return false;
		}
		return true;
	}
	
	private Map<String, String> loadAuthenticatorConfig(String authenticatorKey, String name)
	{
		Map<String, String> ret = new HashMap<String, String>();
		
		ret.put("type", config.getValue(authenticatorKey 
				+ UnityServerConfiguration.AUTHENTICATOR_TYPE));
		ret.put("credential", config.getValue(authenticatorKey
				+ UnityServerConfiguration.AUTHENTICATOR_CREDENTIAL));	
		try
		{
			String vConfigFile = config.getValue(authenticatorKey
							+ UnityServerConfiguration.AUTHENTICATOR_VERIFICATOR_CONFIG);
			String rConfigFile = config.getValue(authenticatorKey
							+ UnityServerConfiguration.AUTHENTICATOR_RETRIEVAL_CONFIG);
			String vJsonConfiguration = vConfigFile == null ? null : serverMan
					.loadConfigurationFile(vConfigFile);
			String rJsonConfiguration = serverMan.loadConfigurationFile(rConfigFile);
			ret.put("vJsonConfiguration", vJsonConfiguration);
			ret.put("rJsonConfiguration", rJsonConfiguration);

		} catch (EngineException e)
		{
			log.error("Cannot read json file", e);
			ErrorPopup.showError(msg,
					msg.getMessage("Authenticators.cannotReadJsonConfig"), e);
			return null;
		}	
		return ret;
	}	
}
