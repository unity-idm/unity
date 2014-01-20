package pl.edu.icm.unity.webadmin.serverman;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;

import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;


/**
 * Show information about all authenticators
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuthenticatorsComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AuthenticatorsComponent.class);
	
	private UnityMessageSource msg;
	private UnityServerConfiguration config;
	private AuthenticationManagement authMan;
	
	
	
	@Autowired
	public AuthenticatorsComponent(UnityMessageSource msg, UnityServerConfiguration config,
			AuthenticationManagement authMan)
	{
		
		this.msg = msg;
		this.config = config;
		this.authMan = authMan;
		initUI();
	}

	private void initUI(){
		
		setCaption(msg.getMessage("Authenticators.caption"));
		setMargin(true);
		setSpacing(true);

		Button reloadAuthButton = new Button(
				msg.getMessage("Authenticators.reloadAuthenticators"));
		reloadAuthButton.setIcon(Images.refresh.getResource());
		reloadAuthButton.addClickListener(new Button.ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{

				reloadAuthenticators();

			}
		});
		addComponent(reloadAuthButton);
	}
	
	private void reloadAuthenticators()
	{
		log.info("Reloading Authenticators");
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{	log.error("Cannot reload configuration",e);
			ErrorPopup.showError(msg,
					msg.getMessage("Endpoints.cannotReloadConfig"), e);
			return;
		}
		
		Collection<AuthenticatorInstance> authenticators;
		try
		{	
			authenticators = authMan.getAuthenticators(null);
		} catch (EngineException e)
		{       
			log.error("Cannot load authenticators",e);
			ErrorPopup.showError(msg,
					msg.getMessage("Authenticators.cannotGetAuthenticators"),
					e);
			return;
		}
		Map<String, AuthenticatorInstance> existing = new HashMap<String, AuthenticatorInstance>();

		for (AuthenticatorInstance ai : authenticators)
		{
			existing.put(ai.getId(), ai);

		}
		Map<String, AuthenticatorInstance> toRemove = new HashMap<>(existing);

		Set<String> authenticatorsList = config
				.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			String name = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_NAME);
			String type = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_TYPE);
			File vConfigFile = config
					.getFileValue(authenticatorKey
							+ UnityServerConfiguration.AUTHENTICATOR_VERIFICATOR_CONFIG,
							false);
			File rConfigFile = config.getFileValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_RETRIEVAL_CONFIG,
					false);
			String credential = config.getValue(authenticatorKey
					+ UnityServerConfiguration.AUTHENTICATOR_CREDENTIAL);

			String vJsonConfiguration = null;
			String rJsonConfiguration = null;
			try
			{
				vJsonConfiguration = vConfigFile == null ? null : FileUtils
						.readFileToString(vConfigFile);
				rJsonConfiguration = FileUtils.readFileToString(rConfigFile);
			} catch (IOException e)
			{	
				log.error("Cannot read json file",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("Endpoints.cannotReadJsonConfig"),
						e);
				return;
			}

			if (!existing.containsKey(name))
			{
				log.info("Add " + name + " [" + type + "]");
				try
				{
					authMan.createAuthenticator(name, type, vJsonConfiguration,
							rJsonConfiguration, credential);
				} catch (EngineException e)
				{
					log.error("Cannot add authenticator",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("Authenticators.cannotAddAuthenticator"),
							e);
					return;
				}

			} else
			{
				log.info("Update " + name + " [" + type + "]");
				try
				{
					authMan.updateAuthenticator(name, vJsonConfiguration,
							rJsonConfiguration);
				} catch (EngineException e)
				{
					log.error("Cannot update authenticator",e);
					ErrorPopup.showError(
							msg,
							msg.getMessage("Authenticators.cannotUpdateAuthenticator"),
							e);
					return;
				}

			}
			toRemove.remove(name);

		}

		for (String auth : toRemove.keySet())
		{
			try
			{
				log.info("Remove " + auth + " authenticator");
				authMan.removeAuthenticator(auth);
			} catch (Exception e)
			{
				log.error("Cannot remove authenticator",e);
				ErrorPopup.showError(
						msg,
						msg.getMessage("Authenticators.cannotRemoveAuthenticator"),
						e);
				return;
			}
		}

	}
}
