/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Allows for removing a requested identity from the request
 * 
 * @author K. Benedyczak
 */
@Component
public class FilterIdentityActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "regFilterIdentity";
	
	public FilterIdentityActionFactory()
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc("identity", 
						"RegTranslationAction.regFilterIdentity.paramDesc.identity",
						Type.EXPRESSION),
				new ActionParameterDesc("type", 
						"RegTranslationAction.regFilterIdentity.paramDesc.identityType",
						Type.UNITY_ID_TYPE)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new FilterIdentityAction(this, parameters);
	}
	
	public static class FilterIdentityAction extends AbstractRegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				FilterIdentityActionFactory.FilterIdentityAction.class);
		private Pattern identityPattern;
		private String type;
		
		public FilterIdentityAction(TranslationActionDescription description, String[] parameters)
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
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			identityPattern = Pattern.compile(parameters[0]);
			type = parameters[1];
		}
	}
}
