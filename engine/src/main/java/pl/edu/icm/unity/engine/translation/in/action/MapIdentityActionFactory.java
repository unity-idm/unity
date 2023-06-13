/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in.action;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;

/**
 * Factory for identity mapping action.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapIdentityActionFactory extends AbstractInputTranslationActionFactory
{
	public static final String NAME = "mapIdentity";
	
	private final IdentityTypesRegistry idsRegistry;
	private final ExternalDataParser parser;
	
	@Autowired
	public MapIdentityActionFactory(IdentityTypesRegistry idsRegistry, ExternalDataParser parser)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"unityIdentityType",
						"TranslationAction.mapIdentity.paramDesc.unityIdentityType",
						Type.UNITY_ID_TYPE, true),
				new ActionParameterDefinition(
						"expression",
						"TranslationAction.mapIdentity.paramDesc.expression",
						Type.EXPRESSION, true,
						MVELExpressionContext.builder().withTitleKey("TranslationAction.mapIdentity.editor.title")
							.withEvalToKey("TranslationAction.mapIdentity.editor.evalTo")
							.withVars(InputTranslationMVELContextKey.toMap()).build()),
				new ActionParameterDefinition(
						"credential requirement",
						"TranslationAction.mapIdentity.paramDesc.credentialRequirement",
						Type.UNITY_CRED_REQ, true),
				new ActionParameterDefinition(
						"effect",
						"TranslationAction.mapIdentity.paramDesc.effect",
						IdentityEffectMode.class, true)});
		this.idsRegistry = idsRegistry;
		this.parser = parser;
	}

	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new MapIdentityAction(parameters, getActionType(), idsRegistry, parser);
	}
	
	
	public static class MapIdentityAction extends InputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapIdentityAction.class);
		private String unityType;
		private String credentialRequirement;
		private Serializable expressionCompiled;
		private IdentityEffectMode mode;
		private IdentityTypeDefinition idTypeResolved;
		private ExternalDataParser parser;

		public MapIdentityAction(String[] params, TranslationActionType desc, IdentityTypesRegistry idsRegistry,
				ExternalDataParser parser) 
		{
			super(desc, params);
			this.parser = parser;
			setParameters(params);
			idTypeResolved = idsRegistry.getByName(unityType);
			if (idTypeResolved.isDynamic() && mode.mayModifyIdentity())
				throw new IllegalArgumentException("Dynamic identity type '" + idTypeResolved + 
						"' can be only used with modes which are matching only.");
		}
		
		@Override
		protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			MappingResult ret = new MappingResult();
			Object value = MVEL.executeExpression(expressionCompiled, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Identity value evaluated to null, skipping");
				return ret;
			}
			List<?> iValues = value instanceof List ? (List<?>)value : Collections.singletonList(value.toString());
			
			for (Object i: iValues)
			{
				
				IdentityParam idParam = parser.parseAsIdentity(idTypeResolved, i, input.getIdpName(),
						currentProfile);
				MappedIdentity mi = new MappedIdentity(mode, idParam, credentialRequirement);
				log.debug("Mapped identity: " + idParam);
				ret.addIdentity(mi);
			}
			return ret;
		}

		private void setParameters(String[] parameters)
		{
			unityType = parameters[0];
			expressionCompiled = MVEL.compileExpression(parameters[1]);
			credentialRequirement = parameters[2];
			mode = IdentityEffectMode.valueOf(parameters[3]);
		}
	}

}
