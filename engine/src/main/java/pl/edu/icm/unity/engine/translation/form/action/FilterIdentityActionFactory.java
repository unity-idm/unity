/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for removing a requested identity from the request
 * 
 * @author K. Benedyczak
 */
@Component(FilterIdentityActionFactory.NAME)
public class FilterIdentityActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "regFilterIdentity";
	
	public FilterIdentityActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("identity", 
						"RegTranslationAction.regFilterIdentity.paramDesc.identity",
						Type.EXPRESSION, true),
				new ActionParameterDefinition("type", 
						"RegTranslationAction.regFilterIdentity.paramDesc.identityType",
						Type.UNITY_ID_TYPE, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new FilterIdentityAction(getActionType(), parameters);
	}
	
	public static class FilterIdentityAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				FilterIdentityActionFactory.FilterIdentityAction.class);
		private Pattern identityPattern;
		private String type;
		
		public FilterIdentityAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Set<IdentityParam> copy = new HashSet<>(state.getIdentities());
			for (IdentityParam i: copy)
				if (identityPattern.matcher(i.getValue()).matches() && i.getTypeId().equals(type))
				{
					log.debug("Filtering identity " + i);
					state.removeIdentity(i);
				}
		}
		
		private void setParameters(String[] parameters)
		{
			identityPattern = Pattern.compile(parameters[0]);
			type = parameters[1];
		}
	}
}
