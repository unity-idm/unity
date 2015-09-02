/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.in.AbstractInputTranslationAction;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Orders to remove stale data, i.e. data that was previously created by the containing profile but is not 
 * valid anymore in the current mapping.
 * @author K. Benedyczak
 */
public class RemoveStaleDataAction extends AbstractInputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, RemoveStaleDataAction.class);
	
	public RemoveStaleDataAction(TranslationActionDescription description, String[] params)
	{
		super(description, params);
		setParameters(params);
	}

	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx,
			String currentProfile) throws EngineException
	{
		MappingResult ret = new MappingResult();
		log.debug("Ordering removal of the stale data");
		ret.setCleanStaleAttributes(true);
		ret.setCleanStaleGroups(true);
		ret.setCleanStaleIdentities(true);
		return ret;
	}
	
	private void setParameters(String[] parameters)
	{
		if (parameters.length != 0)
			throw new IllegalArgumentException("Action requires no parameters");
	}
}
