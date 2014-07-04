/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.IdentityEffectMode;
import pl.edu.icm.unity.server.authn.remote.translation.MappedIdentity;
import pl.edu.icm.unity.server.authn.remote.translation.MappingResult;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Defines Unity identity from a given expression. The engine can either match by or create the defined identity. 
 *   
 * @author K. Benedyczak
 */
public class MapIdentityAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapIdentityAction.class);
	private String unityType;
	private String credentialRequirement;
	private Serializable expressionCompiled;
	private IdentityEffectMode mode;

	public MapIdentityAction(String[] params, TranslationActionDescription desc)
	{
		super(desc, params);
		setParameters(params);
	}
	
	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx,
			String currentProfile) throws EngineException
	{
		Object value = MVEL.executeExpression(expressionCompiled, mvelCtx);
		List<?> iValues = value instanceof List ? (List<?>)value : Collections.singletonList(value.toString());
		
		MappingResult ret = new MappingResult();
		for (Object i: iValues)
		{
			IdentityParam idParam = new IdentityParam(unityType, i.toString(), input.getIdpName(),
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
