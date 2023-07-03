/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;

/**
 * Sets a non-default state for the requester.
 * @author K. Benedyczak
 */
@Component
public class SetEntityStateActionFactory extends AbstractRegistrationTranslationActionFactory
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FORMS, SetEntityStateActionFactory.class);
	public static final String NAME = "setState";

	/**
	 * Same as {@link EntityState} but with one option removed
	 */
	public enum EntityStateLimited {valid, authenticationDisabled, disabled};
	
	public SetEntityStateActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"state",
						"RegTranslationAction.setState.paramDesc.state",
						EntityStateLimited.class, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new SetEntityStateAction(getActionType(), parameters);
	}
	
	public static class SetEntityStateAction extends RegistrationTranslationAction
	{
		private EntityState state;
		
		public SetEntityStateAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				RegistrationContext context, String currentProfile) throws EngineException
		{
			state.setEntityState(this.state);
			log.debug("Will set user's MFA preference to: {}", state);
		}
		
		private void setParameters(String[] parameters)
		{
			state = EntityState.valueOf(parameters[0]);
		}
	}
}
