/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AbstractVerificator;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;

/**
 * Base class that is nearly mandatory for all remote verificators. The remote verificator should extend it 
 * by implementing a {@link CredentialExchange} of choice. The implementation should obtain the 
 * {@link RemotelyAuthenticatedInput} (the actual coding should be done here) and before returning it should
 * be processed by {@link #getResult(RemotelyAuthenticatedInput)} to obtain the final authentication result.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractRemoteVerificator extends AbstractVerificator
{
	private TranslationProfileManagement profileManagement;
	private AttributesManagement attrMan;
	
	public AbstractRemoteVerificator(String name, String description, String exchangeId, 
			TranslationProfileManagement profileManagement, AttributesManagement attrMan)
	{
		super(name, description, exchangeId);
		this.profileManagement = profileManagement;
		this.attrMan = attrMan;
	}

	/**
	 * This method is calling {@link #processRemoteInput(RemotelyAuthenticatedInput)} and then
	 * {@link #assembleAuthenticationResult(RemotelyAuthenticatedContext)}.
	 * Usually it is the only one that is used in subclasses, when {@link RemotelyAuthenticatedInput} 
	 * is obtained in an implementation specific way.
	 * 
	 * @param input
	 * @return
	 * @throws EngineException 
	 */
	protected AuthenticationResult getResult(RemotelyAuthenticatedInput input, String profile) 
			throws AuthenticationException
	{
		RemoteVerificatorUtil util = new RemoteVerificatorUtil(identityResolver, profileManagement, attrMan);
		return util.getResult(input, profile);
	}
}
