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

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;

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

	private ExternalDataParser dataParser;

	@Autowired
	public CreatePersistentIdentityActionFactory(IdentityTypesRegistry idTypesReg, ExternalDataParser dataParser)
	{
		super(NAME,
				new ActionParameterDefinition("identityType",
						"TranslationAction.createPersistedIdentity.paramDesc.idType", Type.UNITY_ID_TYPE, true),
				new ActionParameterDefinition("expression",
						"TranslationAction.createPersistedIdentity.paramDesc.idValueExpression", Type.EXPRESSION, true,
						MVELExpressionContext.builder()
								.withTitleKey("TranslationAction.createPersistedIdentity.editor.title")
								.withEvalToKey("TranslationAction.createPersistedIdentity.editor.evalTo")
								.withVars(OutputTranslationMVELContextKey.toMap()).build()));
		this.idTypesReg = idTypesReg;
		this.dataParser = dataParser;
	}

	@Override
	public CreatePersistentIdentityAction getInstance(String... parameters)
	{
		return new CreatePersistentIdentityAction(parameters, getActionType(), idTypesReg, dataParser);
	}

	public static class CreatePersistentIdentityAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreatePersistentIdentityAction.class);
		private IdentityTypeDefinition idType;
		private Serializable idValueExpression;
		private ExternalDataParser dataParser;

		public CreatePersistentIdentityAction(String[] params, TranslationActionType desc,
				IdentityTypesRegistry idTypesReg, ExternalDataParser dataParser)
		{
			super(desc, params);
			this.dataParser = dataParser;
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
			
			//for output profile we can't confirm - not yet implemented and rather not needed.
			IdentityParam newId = dataParser.parseAsConfirmedIdentity(idType, value, null, currentProfile);
			
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
