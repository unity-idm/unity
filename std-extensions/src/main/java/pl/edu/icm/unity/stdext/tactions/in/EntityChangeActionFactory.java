/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.ProfileType;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.types.EntityScheduledOperation;

/**
 * Factory of entity status change actions.
 * @author K. Benedyczak
 */
@Component
public class EntityChangeActionFactory implements TranslationActionFactory
{
	public static final String NAME = "changeStatus";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescriptionKey()
	{
		return "TranslationAction.changeStatus.desc";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] {
				new ActionParameterDesc(
						"schedule change",
						"TranslationAction.changeStatus.paramDesc.scheduleChange",
						1, 1, EntityScheduledOperation.class),
				new ActionParameterDesc(
						"scheduled after days",
						"TranslationAction.changeStatus.paramDesc.scheduledTime",
						1, 1, Type.DAYS)};
	}

	@Override
	public InputTranslationAction getInstance(String... parameters) throws EngineException
	{
		return new EntityChangeAction(this, parameters);
	}

	@Override
	public ProfileType getSupportedProfileType()
	{
		return ProfileType.INPUT;
	}
}
