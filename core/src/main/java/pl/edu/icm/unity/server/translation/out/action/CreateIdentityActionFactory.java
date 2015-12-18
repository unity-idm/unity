/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out.action;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.IdentityParam;

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
		super(NAME, new ActionParameterDesc(
				"identityType",
				"TranslationAction.createIdentity.paramDesc.idType",
				Type.EXPRESSION),
		new ActionParameterDesc(
				"expression",
				"TranslationAction.createIdentity.paramDesc.idValueExpression",
				Type.EXPRESSION));
	}
	
	@Override
	public CreateIdentityAction getInstance(String... parameters) throws EngineException
	{
		return new CreateIdentityAction(parameters, this);
	}
	
	public static class CreateIdentityAction extends AbstractOutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreateIdentityAction.class);
		private String idTypeString;
		private Serializable idValueExpression;

		public CreateIdentityAction(String[] params, TranslationActionDescription desc) throws EngineException
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Object valueO = MVEL.executeExpression(idValueExpression, mvelCtx);
			if (valueO == null)
			{
				log.debug("Identity value evaluated to null, skipping");
				return;
			}
			String value = valueO.toString();
			for (IdentityParam existing: result.getIdentities())
			{
				if (existing.getTypeId().equals(idTypeString))
				{
					if (value.equals(existing.getValue()))
					{
						log.trace("Identity already exists, skipping");
						return;
					}
				}
			}
			
			IdentityParam newId = new IdentityParam();
			newId.setValue(value);
			newId.setTypeId(idTypeString);
			result.getIdentities().add(newId);
			log.debug("Created a new volatile identity: " + newId);
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			idTypeString = parameters[0];
			idValueExpression = MVEL.compileExpression(parameters[1]);
		}
	}

}
