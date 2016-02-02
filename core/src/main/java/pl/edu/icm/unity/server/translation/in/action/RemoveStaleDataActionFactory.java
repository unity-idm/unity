/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in.action;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Maps multiple attributes only by providing new names, values are unchanged.
 * @author K. Benedyczak
 */
@Component
public class RemoveStaleDataActionFactory extends AbstractInputTranslationActionFactory
{
	public static final String NAME = "removeStaleData";
	
	public RemoveStaleDataActionFactory()
	{
		super(NAME);
	}
	
	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new RemoveStaleDataAction(getActionType(), parameters);
	}
	
	public static class RemoveStaleDataAction extends InputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, RemoveStaleDataAction.class);
		
		public RemoveStaleDataAction(TranslationActionType description, String[] params)
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

}
