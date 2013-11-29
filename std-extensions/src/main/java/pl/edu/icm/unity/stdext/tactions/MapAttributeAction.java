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
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Maps attributes matching a given regular expression (param1) to a new name (param2) which can use group references
 * from the pattern in a given group (param3).
 *   
 * @author K. Benedyczak
 */
public class MapAttributeAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapAttributeAction.class);
	private Pattern toReplace;
	private String replacement;
	private String targetGroup;

	public MapAttributeAction(String[] params)
	{
		setParameters(params);
	}
	
	@Override
	public String getName()
	{
		return MapAttributeActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		Map<String, RemoteAttribute> attributes = input.getAttributes();
		Set<String> keys = new HashSet<>(attributes.keySet());
		
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
				log.trace("Attribute " + key + " doesn't match");
				continue;
			}
			m.appendTail(sb);
			
			log.debug("Translating attribute " + key + " -> " + sb);
			RemoteAttribute changed = attributes.remove(key);
			attributes.put(sb.toString(), changed);
			changed.getMetadata().put(RemoteInformationBase.UNITY_ATTRIBUTE, sb.toString());
			changed.getMetadata().put(RemoteInformationBase.UNITY_GROUP, targetGroup);
		}
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {toReplace.pattern(), replacement, targetGroup};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactely 3 parameters");
		toReplace = Pattern.compile(parameters[0]);
		replacement = parameters[1];
		targetGroup = parameters[2];
	}
}
