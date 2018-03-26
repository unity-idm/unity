/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Creates new outgoing identities.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreatePersistentIdentityActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "createPersistedIdentity";
	
	private IdentityTypesRegistry idTypesReg;

	@Autowired
	public CreatePersistentIdentityActionFactory(IdentityTypesRegistry idTypesReg)
	{
		super(NAME, new ActionParameterDefinition(
				"identityType",
				"TranslationAction.createPersistedIdentity.paramDesc.idType",
				Type.UNITY_ID_TYPE, true),
		new ActionParameterDefinition(
				"expression",
				"TranslationAction.createPersistedIdentity.paramDesc.idValueExpression",
				Type.EXPRESSION, true));
		this.idTypesReg = idTypesReg;
	}

	@Override
	public CreatePersistentIdentityAction getInstance(String... parameters)
	{
		return new CreatePersistentIdentityAction(parameters, getActionType(), idTypesReg);
	}

	public static class CreatePersistentIdentityAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreatePersistentIdentityAction.class);
		private IdentityTypeDefinition idType;
		private Serializable idValueExpression;

		public CreatePersistentIdentityAction(String[] params, TranslationActionType desc,
				IdentityTypesRegistry idTypesReg)
		{
			super(desc, params);
			setParameters(params, idTypesReg);
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
			
			IdentityParam newId = idType.convertFromString(value, null, currentProfile);
			//for output profile we can't confirm - not yet implemented and rather not needed.
			if (idType.isEmailVerifiable())
				newId.setConfirmationInfo(new ConfirmationInfo(true));
			
			if (result.removeIdentityToPersistByType(idType.getId()))
			{
				result.removeIdentityByType(idType.getId());
				log.trace("Identity to persist type '" + idType.getId()
						+ "' already exists, overwrite");
			}
			
			result.getIdentities().add(newId);
			result.getIdentitiesToPersist().add(newId);
			log.debug("Created a new persisted identity: " + newId);
		}

		private void setParameters(String[] parameters, IdentityTypesRegistry idTypesReg)
		{
			idValueExpression = MVEL.compileExpression(parameters[1]);

			idType = idTypesReg.getByName(parameters[0]);
			if (idType.isDynamic())
				throw new IllegalArgumentException("The identity type " + parameters[0] + 
						" is dynamic so it can not be persisted");
		}
	}

}
