package pl.edu.icm.unity.webadmin.serverman;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.webui.common.ErrorPopup;

public class SingleAuthenticatorComponent extends SingleComponent
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			SingleAuthenticatorComponent.class);
	AuthenticatorInstance authenticator;
	AuthenticationManagement authMan;
	public SingleAuthenticatorComponent(AuthenticationManagement authMan,AuthenticatorInstance authenticator,UnityServerConfiguration config,
			UnityMessageSource msg, String status)
	{
		super(config, msg, status,"Authenticators");
		this.authenticator=authenticator;
		this.authMan=authMan;
		setStatus(status);
	}
	
	@Override
	protected void updateHeader()
	{
		super.updateHeader(authenticator.getId());
	}
	
	@Override
	protected void undeploy()
	{
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			log.error("Cannot reload configuration",e);
			ErrorPopup.showError(msg,
					msg.getMessage(msgPrefix+"."+"cannotReloadConfig"), e);
			return;
		}

		log.info("Remove " + authenticator.getId() + " endpoint");
		try
		{
			authMan.removeAuthenticator(authenticator.getId());
		} catch (Exception e)
		{
        		log.error("Cannot remove authenticator",e);
       			ErrorPopup.showError(msg,
					msg.getMessage(msgPrefix+"."+"cannotRemove"), e);
			return;

		}
		
		
		boolean inConfig=false;
		
		
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
		Set<String> existing = new HashSet<String>();

		for (AuthenticatorInstance ai : authenticators)
		{
			existing.add(ai.getId());
			

		}
		
		Set<String> authenticatorsList = config
				.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			if (config.getValue(authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_NAME)
					.equals(authenticator.getId()))
			{
					inConfig=true;
			}
		
		}
		
		
		
		if(inConfig)
		{
			setStatus(STATUS_UNDEPLOYED);
		}
		else
		{
			setVisible(false);
		}
		
	}
	
	
	

}
