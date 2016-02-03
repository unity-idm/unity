/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Action used instead of a real action when it is misconfigured.
 * @author K. Benedyczak
 */
public class BlindStopperRegistrationAction extends RegistrationTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BlindStopperRegistrationAction.class);
	
	public BlindStopperRegistrationAction(TranslationActionType description, String[] params)
	{
		super(description, params);
	}

	@Override
	protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
			String currentProfile) throws EngineException
	{
		log.warn("Skipping invocation of a invalid action " + getName());
	}
}
