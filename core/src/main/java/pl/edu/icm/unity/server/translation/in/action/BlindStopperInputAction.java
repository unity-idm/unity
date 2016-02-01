/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in.action;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.in.AbstractInputTranslationAction;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Action used instead of a real action when it is misconfigured.
 * @author K. Benedyczak
 */
public class BlindStopperInputAction extends AbstractInputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BlindStopperInputAction.class);
	
	public BlindStopperInputAction(TranslationActionDescription description, String[] params)
	{
		super(description, params);
	}

	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx,
			String currentProfile) throws EngineException
	{
		log.warn("Skipping invocation of a invalid action " + getActionDescription().getName());
		return new MappingResult();
	}
}
