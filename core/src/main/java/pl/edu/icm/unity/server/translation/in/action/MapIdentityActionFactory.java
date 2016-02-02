/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in.action;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.MappedIdentity;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Factory for identity mapping action.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapIdentityActionFactory extends AbstractInputTranslationActionFactory
{
	public static final String NAME = "mapIdentity";
	
	private IdentityTypesRegistry idsRegistry;
	
	@Autowired
	public MapIdentityActionFactory(IdentityTypesRegistry idsRegistry)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"unityIdentityType",
						"TranslationAction.mapIdentity.paramDesc.unityIdentityType",
						Type.UNITY_ID_TYPE),
				new ActionParameterDefinition(
						"expression",
						"TranslationAction.mapIdentity.paramDesc.expression",
						Type.EXPRESSION),
				new ActionParameterDefinition(
						"credential requirement",
						"TranslationAction.mapIdentity.paramDesc.credentialRequirement",
						Type.UNITY_CRED_REQ),
				new ActionParameterDefinition(
						"effect",
						"TranslationAction.mapIdentity.paramDesc.effect",
						IdentityEffectMode.class)});
		this.idsRegistry = idsRegistry;
	}

	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new MapIdentityAction(parameters, getActionType(), idsRegistry);
	}
	
	
	public static class MapIdentityAction extends InputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapIdentityAction.class);
		private String unityType;
		private String credentialRequirement;
		private Serializable expressionCompiled;
		private IdentityEffectMode mode;
		private IdentityTypeDefinition idTypeResolved;

		public MapIdentityAction(String[] params, TranslationActionType desc, IdentityTypesRegistry idsRegistry) 
		{
			super(desc, params);
			setParameters(params);
			try
			{
				idTypeResolved = idsRegistry.getByName(unityType);
			} catch (IllegalTypeException e)
			{
				throw new IllegalArgumentException("Unknown identity type", e);
			}
		}
		
		@Override
		protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			MappingResult ret = new MappingResult();
			Object value = MVEL.executeExpression(expressionCompiled, mvelCtx);
			if (value == null)
			{
				log.debug("Identity value evaluated to null, skipping");
				return ret;
			}
			List<?> iValues = value instanceof List ? (List<?>)value : Collections.singletonList(value.toString());
			
			for (Object i: iValues)
			{
				IdentityParam idParam = idTypeResolved.convertFromString(i.toString(), input.getIdpName(),
						currentProfile);
				MappedIdentity mi = new MappedIdentity(mode, idParam, credentialRequirement);
				log.debug("Mapped identity: " + idParam);
				ret.addIdentity(mi);
			}
			return ret;
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 4)
				throw new IllegalArgumentException("Action requires exactly 4 parameters");
			unityType = parameters[0];
			expressionCompiled = MVEL.compileExpression(parameters[1]);
			credentialRequirement = parameters[2];
			mode = IdentityEffectMode.valueOf(parameters[3]);
		}
	}

}
