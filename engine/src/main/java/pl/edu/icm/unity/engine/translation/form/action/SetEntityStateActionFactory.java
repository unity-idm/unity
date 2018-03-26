/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Sets a non-default state for the requester.
 * @author K. Benedyczak
 */
@Component
public class SetEntityStateActionFactory extends AbstractRegistrationTranslationActionFactory
{
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
				String currentProfile) throws EngineException
		{
			state.setEntityState(this.state);
		}
		
		private void setParameters(String[] parameters)
		{
			state = EntityState.valueOf(parameters[0]);
		}
	}
}
