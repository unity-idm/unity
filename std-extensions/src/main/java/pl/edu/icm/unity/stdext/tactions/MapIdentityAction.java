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
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Maps identities matching a given regular expression (param1) to a new name (param2) which can use group references
 * from the pattern. Also assigns a local credentialRequirement.
 *   
 * @author K. Benedyczak
 */
public class MapIdentityAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapIdentityAction.class);
	private Pattern toReplace;
	private String replacement;
	private String credentialRequirement;

	public MapIdentityAction(String[] params)
	{
		setParameters(params);
	}
	
	@Override
	public String getName()
	{
		return MapIdentityActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		Map<String, RemoteIdentity> identities = input.getIdentities();
		Set<String> keys = new HashSet<>(identities.keySet());
		
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
				log.debug("Identity " + key + " doesn't match");
				continue;
			}
			
			m.appendTail(sb);
			log.debug("Translating identity " + key + " -> " + sb);
			
			RemoteIdentity changed = identities.remove(key);
			identities.put(sb.toString(), changed);
			changed.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY, sb.toString());
			changed.getMetadata().put(RemoteInformationBase.UNITY_IDENTITY_CREDREQ, credentialRequirement);
		}
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {toReplace.pattern(), replacement, credentialRequirement};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactely 3 parameters");
		toReplace = Pattern.compile(parameters[0]);
		replacement = parameters[1];
		credentialRequirement = parameters[2];
	}
}
