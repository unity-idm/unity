/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.types.EntityState;

/**
 * Sets a non-default state for the requester.
 * @author K. Benedyczak
 */
@Component
public class SetEntityStateActionFactory extends AbstractTranslationActionFactory
{
	public static final String NAME = "setState";

	/**
	 * Same as {@link EntityState} but with one option removed
	 */
	public enum EntityStateLimited {valid, authenticationDisabled, disabled};
	
	public SetEntityStateActionFactory()
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc(
						"state",
						"RegTranslationAction.setState.paramDesc.state",
						EntityStateLimited.class)
		});
	}

	@Override
	public TranslationAction getInstance(String... parameters)
	{
		return new SetEntityStateAction(this, parameters);
	}
	
	public static class SetEntityStateAction extends AbstractRegistrationTranslationAction
	{
		private EntityState state;
		
		public SetEntityStateAction(TranslationActionDescription description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			state.setInitialEntityState(this.state);
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			state = EntityState.valueOf(parameters[0]);
		}
	}
}
