/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.translation.in.EntityChange;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityScheduledOperation;

/**
 * Schedules an entity operation.
 * @author K. Benedyczak
 */
@Component
public class ScheduleEntityChangeActionFactory extends AbstractTranslationActionFactory
{
	public static final String NAME = "scheduleChange";

	public ScheduleEntityChangeActionFactory()
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc(
						"schedule change",
						"TranslationAction.scheduleChange.paramDesc.scheduleChange",
						1, 1, EntityScheduledOperation.class),
				new ActionParameterDesc(
						"scheduled after days",
						"TranslationAction.scheduleChange.paramDesc.scheduledTime",
						1, 1, Type.DAYS)
		});
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new ScheduleEntityChangeAction(this, parameters);
	}
	
	public static class ScheduleEntityChangeAction extends AbstractRegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				ScheduleEntityChangeActionFactory.ScheduleEntityChangeAction.class);
		private Date changeDate;
		private EntityScheduledOperation scheduledOp;
		
		public ScheduleEntityChangeAction(TranslationActionDescription description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			EntityChange change = new EntityChange(scheduledOp, changeDate);
			log.debug("Entity scheduled operation: " + scheduledOp);
			state.setEntityChange(change);
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			changeDate = new Date(System.currentTimeMillis() + 
					Long.parseLong(parameters[1]) * 24 * 3600 * 1000L);
			scheduledOp = EntityScheduledOperation.valueOf(parameters[0]);
		}
	}
}
