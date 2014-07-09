/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;

/**
 * Minimal base for output translation actions. Ensures that logging NDC is properly pushed and popped.
 * @author K. Benedyczak
 */
public abstract class AbstractOutputTranslationAction extends AbstractTranslationAction implements OutputTranslationAction
{
	public AbstractOutputTranslationAction(TranslationActionDescription description,
			String[] params)
	{
		super(description, params);
	}

	@Override
	public void invoke(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		try
		{
			NDC.push("[" + input + "]");
			invokeWrapped(input, mvelCtx, currentProfile, result);
		} finally
		{
			NDC.pop();			
		}
	}
	
	protected abstract void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException;
	

}
