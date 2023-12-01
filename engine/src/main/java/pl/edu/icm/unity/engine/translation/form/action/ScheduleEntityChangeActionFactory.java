/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.form.GroupRestrictedFormValidationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.in.EntityChange;

/**
 * Schedules an entity operation.
 * @author K. Benedyczak
 */
@Component
public class ScheduleEntityChangeActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "scheduleChange";

	public ScheduleEntityChangeActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"schedule change",
						"RegTranslationAction.scheduleChange.paramDesc.scheduleChange",
						EntityScheduledOperation.class, true),
				new ActionParameterDefinition(
						"scheduled after days",
						"RegTranslationAction.scheduleChange.paramDesc.scheduledTime",
						Type.DAYS, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new ScheduleEntityChangeAction(getActionType(), parameters);
	}
	
	public static class ScheduleEntityChangeAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				ScheduleEntityChangeActionFactory.ScheduleEntityChangeAction.class);
		private Date changeDate;
		private EntityScheduledOperation scheduledOp;
		
		public ScheduleEntityChangeAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				RegistrationContext context, String currentProfile) throws EngineException
		{
			EntityChange change = new EntityChange(scheduledOp, changeDate);
			log.debug("Entity scheduled operation: " + scheduledOp);
			state.setEntityChange(change);
		}
		
		@Override
		public void validateGroupRestrictedForm(GroupRestrictedFormValidationContext context) throws ActionValidationException
		{
		}
		
		private void setParameters(String[] parameters)
		{
			changeDate = new Date(System.currentTimeMillis() + 
					Long.parseLong(parameters[1]) * 24 * 3600 * 1000L);
			scheduledOp = EntityScheduledOperation.valueOf(parameters[0]);
		}
	}
}
