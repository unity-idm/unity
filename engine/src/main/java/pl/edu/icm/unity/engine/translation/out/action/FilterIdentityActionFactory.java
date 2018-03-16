/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Filter outgoing identities by name and or type. Name filter is specified using regular expressions
 *   
 * @author K. Benedyczak
 */
@Component(FilterIdentityActionFactory.NAME)
public class FilterIdentityActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "filterIdentity";
	
	public FilterIdentityActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"identity",
				"TranslationAction.filterIdentity.paramDesc.idType",
				Type.UNITY_ID_TYPE, true),
		new ActionParameterDefinition(
				"identityValueRegexp",
				"TranslationAction.filterIdentity.paramDesc.idValueReqexp",
				Type.EXPRESSION, true));
	}

	@Override
	public FilterIdentityAction getInstance(String... parameters)
	{
		return new FilterIdentityAction(parameters, getActionType());
	}
	
	public static class FilterIdentityAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FilterIdentityAction.class);
		private String identity;
		private Pattern idValueRegexp;

		public FilterIdentityAction(String[] params, TranslationActionType desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			
			Collection<IdentityParam> res = result.removeIdentityByTypeAndValueMatch(identity, idValueRegexp);
			if (!res.isEmpty())
			{
				for (IdentityParam id : res)
					log.debug("Filtering the identity " + id.toString());
			}

			res = result.removeIdentityToPersistByTypeAndValueMatch(identity,
					idValueRegexp);
			if (!res.isEmpty())
			{
				for (IdentityParam id : res)
					log.debug("Filtering the identity to persist "
							+ id.toString());
			}
			
		}

		private void setParameters(String[] parameters)
		{
			identity = parameters[0];
			idValueRegexp = parameters[1] == null ? null : Pattern.compile(parameters[1]);
		}

	}

}
