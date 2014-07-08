/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;

/**
 * Minimal base for input translation actions. Ensures that logging NDC is properly pushed and popped.
 * @author K. Benedyczak
 */
public abstract class AbstractInputTranslationAction extends AbstractTranslationAction implements InputTranslationAction
{
	public AbstractInputTranslationAction(TranslationActionDescription description,
			String[] params)
	{
		super(description, params);
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
	

}
