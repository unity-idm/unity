/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
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

	private AuthenticatorInstance authenticator;
	private AuthenticatorManagement authMan;

	@Autowired
	public AuthenticatorComponent(AuthenticatorManagement authMan, ServerManagement serverMan,
			UnityServerConfiguration config, UnityMessageSource msg)
	{
		super(config, serverMan , msg);
		this.authMan = authMan;
	}

	public AuthenticatorComponent init(AuthenticatorInstance authenticator, Status status)
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
		
		addConfigPanel(msg.getMessage("Authenticators.verificatorJsonConfiguration"), 
				authenticator.getVerificatorConfiguration());
		addConfigPanel(msg.getMessage("Authenticators.retrievalJsonConfiguration"), 
				authenticator.getRetrievalConfiguration());
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
			NotificationPopup.showError(msg,
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
		Map<String, String> data = getAuthenticatorConfig(name);
		if (data == null)
		{
			return false;
		}
		try
		{
			this.authenticator = authMan.createAuthenticator(name, data.get("type"), data.get("vJsonConfiguration"),
					data.get("rJsonConfiguration"), data.get("credential"));
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
				NotificationPopup.showSuccess(msg, "", msg.getMessage(
						"Authenticators.reloadSuccess", id));
			}
		}
	}
	
	private boolean reloadAuthenticator(String name)
	{
		Map<String, String> data = getAuthenticatorConfig(name);
		if (data == null)
		{
			return false;
		}
			
		try
		{
			authMan.updateAuthenticator(name, data.get("vJsonConfiguration"),
					data.get("rJsonConfiguration"), data.get("credential"));
		} catch (Exception e)
		{
			log.error("Cannot update authenticator", e);
			NotificationPopup.showError(msg, msg.getMessage(
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
		} catch (Exception e)
		{
			log.error("Cannot load authenticators", e);
			NotificationPopup.showError(msg,msg.getMessage("error"),
					msg.getMessage("Authenticators.cannotLoadList"));
			return false;
		}
		return true;
	}
	
	private Map<String, String> getAuthenticatorConfig(String name)
	{
		String authenticatorKey = null;
		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticator: authenticatorsList)
		{

			String cname = config.getValue(authenticator + UnityServerConfiguration.AUTHENTICATOR_NAME);
			if (name.equals(cname))
			{
				authenticatorKey = authenticator;
			}	
		}
		if (authenticatorKey == null)
		{
			return null;
		}
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
			String rJsonConfiguration = rConfigFile == null ? null : serverMan
					.loadConfigurationFile(rConfigFile);
			ret.put("vJsonConfiguration", vJsonConfiguration);
			ret.put("rJsonConfiguration", rJsonConfiguration);

		} catch (Exception e)
		{
			log.error("Cannot read json file", e);
			NotificationPopup.showError(msg,
					msg.getMessage("Authenticators.cannotReadJsonConfig"), e);
			return null;
		}	
		return ret;
	}	
}
