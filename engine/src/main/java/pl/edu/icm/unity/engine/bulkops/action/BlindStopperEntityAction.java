/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops.action;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Action used instead of a real action when it is misconfigured.
 * @author K. Benedyczak
 */
public class BlindStopperEntityAction extends EntityAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, BlindStopperEntityAction.class);
	
	public BlindStopperEntityAction(TranslationActionType sourceActionDescription, String[] sourceActionParams)
	{
		super(new TranslationActionType(sourceActionDescription.getSupportedProfileType(),
				sourceActionDescription.getDescriptionKey(),
				sourceActionDescription.getName(),
				new ActionParameterDefinition[0]), new String[0]);
	}

	@Override
	public void invoke(Entity entity)
	{
		log.warn("Skipping invocation of a invalid action " + getName());
	}

}
