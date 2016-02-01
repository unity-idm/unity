/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.translation.out;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Action used instead of a real action when it is misconfigured.
 * @author K. Benedyczak
 */
public class BlindStopperOutputAction extends AbstractOutputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BlindStopperOutputAction.class);
	
	public BlindStopperOutputAction(TranslationActionDescription description, String[] params)
	{
		super(description, params);
	}

	@Override
	protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		log.warn("Skipping invocation of a invalid action " + getActionDescription().getName());
	}
}
