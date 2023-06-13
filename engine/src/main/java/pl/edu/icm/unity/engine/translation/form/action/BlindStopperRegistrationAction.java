/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;

/**
 * Action used instead of a real action when it is misconfigured.
 * @author K. Benedyczak
 */
public class BlindStopperRegistrationAction extends RegistrationTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BlindStopperRegistrationAction.class);
	
	public BlindStopperRegistrationAction(TranslationActionType sourceActionDescription, String[] sourceActionParams)
	{
		super(new TranslationActionType(sourceActionDescription.getSupportedProfileType(),
				sourceActionDescription.getDescriptionKey(),
				sourceActionDescription.getName(),
				new ActionParameterDefinition[0]), new String[0]);
	}

	@Override
	protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
			RegistrationContext context, String currentProfile) throws EngineException
	{
		log.warn("Skipping invocation of a invalid action " + getName());
	}
}
