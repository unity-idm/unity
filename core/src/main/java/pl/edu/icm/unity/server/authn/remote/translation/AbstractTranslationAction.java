/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;


/**
 * Minimal base for translation actions. Ensures that logging NDC is properly pushed and popped.
 * @author K. Benedyczak
 */
public abstract class AbstractTranslationAction implements TranslationAction
{
	@Override
	public void invoke(RemotelyAuthenticatedInput input) throws EngineException
	{
		try
		{
			NDC.push("[" + input + "]");
			invokeWrapped(input);
		} finally
		{
			NDC.pop();			
		}
	}
	
	protected abstract void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException;
}
