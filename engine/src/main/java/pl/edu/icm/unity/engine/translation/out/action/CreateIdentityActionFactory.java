/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
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
 * Creates new outgoing identities.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreateIdentityActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "createIdentity";
	
	public CreateIdentityActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"identityType",
				"TranslationAction.createIdentity.paramDesc.idType",
				Type.TEXT, true),
		new ActionParameterDefinition(
				"expression",
				"TranslationAction.createIdentity.paramDesc.idValueExpression",
				Type.EXPRESSION, true));
	}
	
	@Override
	public CreateIdentityAction getInstance(String... parameters)
	{
		return new CreateIdentityAction(parameters, getActionType());
	}
	
	public static class CreateIdentityAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreateIdentityAction.class);
		private String idTypeString;
		private Serializable idValueExpression;

		public CreateIdentityAction(String[] params, TranslationActionType desc)
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Object valueO = MVEL.executeExpression(idValueExpression, mvelCtx, new HashMap<>());
			if (valueO == null)
			{
				log.debug("Identity value evaluated to null, skipping");
				return;
			}
			String value = valueO.toString();

			if (result.removeIdentityByType(idTypeString))
			{
				// check if identity is also in identity to
				// persist and remove them.
				result.removeIdentityToPersistByType(idTypeString);
				log.trace("Identity type '" + idTypeString
						+ "' already exists, overwrite");
			}
			
			IdentityParam newId = new IdentityParam(idTypeString, value);
			newId.setTranslationProfile(currentProfile);
			result.getIdentities().add(newId);
			log.debug("Created a new volatile identity: " + newId);
		}

		private void setParameters(String[] parameters)
		{
			idTypeString = parameters[0];
			idValueExpression = MVEL.compileExpression(parameters[1]);
		}
	}

}
