/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Maps groups matching a given regular expression (param1) to a new name (param2) which can use group references
 * from the pattern.
 *   
 * @author K. Benedyczak
 */
public class MapGroupAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapGroupAction.class);
	private Pattern toReplace;
	private String replacement;

	public MapGroupAction(String[] params)
	{
		setParameters(params);
	}
	
	@Override
	public String getName()
	{
		return MapGroupActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		Map<String, RemoteGroupMembership> groups = input.getGroups();
		Set<String> keys = new HashSet<>(groups.keySet());
		
		for (String key: keys)
		{
			Matcher m = toReplace.matcher(key);
			boolean matches = false;
			StringBuffer sb = new StringBuffer();
			while(m.find())
			{
				matches = true;
				m.appendReplacement(sb, replacement);
			}
			if (!matches)
			{
				log.trace("Identity " + key + " doesn't match");
				continue;
			}
			m.appendTail(sb);
			
			log.debug("Translating group " + key + " -> " + sb);
			RemoteGroupMembership changed = groups.remove(key);
			groups.put(sb.toString(), changed);
			changed.getMetadata().put(RemoteInformationBase.UNITY_GROUP, sb.toString());
		}
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {toReplace.pattern(), replacement};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 2)
			throw new IllegalArgumentException("Action requires exactely 2 parameters");
		toReplace = Pattern.compile(parameters[0]);
		replacement = parameters[1];
	}
}
