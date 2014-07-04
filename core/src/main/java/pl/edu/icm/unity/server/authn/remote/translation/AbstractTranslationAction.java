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
	protected TranslationActionDescription description;
	protected String[] params;
	
	public AbstractTranslationAction(TranslationActionDescription description, String[] params)
	{
		this.description = description;
		this.params = params;
	}

	@Override
	public TranslationActionDescription getActionDescription()
	{
		return description;
	}
	
	@Override
	public final MappingResult invoke(RemotelyAuthenticatedInput input, Object mvelCtx, 
			String currentProfile) throws EngineException
	{
		try
		{
			NDC.push("[" + input + "]");
			return invokeWrapped(input, mvelCtx, currentProfile);
		} finally
		{
			NDC.pop();			
		}
	}
	
	protected abstract MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, 
			String currentProfile) throws EngineException;
	
	@Override
	public String[] getParameters()
	{
		return params;
	}
}
