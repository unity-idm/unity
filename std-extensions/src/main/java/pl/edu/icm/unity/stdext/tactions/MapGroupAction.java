/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.io.Serializable;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.MappingResult;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Create group mappings.
 *   
 * @author K. Benedyczak
 */
public class MapGroupAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapGroupAction.class);
	private Serializable expressionCompiled;

	public MapGroupAction(String[] params, TranslationActionDescription desc)
	{
		super(desc, params);
		setParameters(params);
	}
	
	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, String currentProfile) 
			throws EngineException
	{
		MappingResult ret = new MappingResult();
		Object result = MVEL.executeExpression(expressionCompiled, mvelCtx);
		if (result instanceof Collection<?>)
		{
			Collection<?> mgs = (Collection<?>) result;
			for (Object mg: mgs)
			{
				log.debug("Mapped group: " + mg.toString());
				ret.addGroup(mg.toString());
			}
		} else
		{
			log.debug("Mapped group: " + result.toString());
			ret.addGroup(result.toString());
		}
		return ret;
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 1)
			throw new IllegalArgumentException("Action requires exactly 1 parameter");
		expressionCompiled = MVEL.compileExpression(parameters[0]);
	}
}
