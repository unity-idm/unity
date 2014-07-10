/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Add the user to a specified group.
 *   
 * @author K. Benedyczak
 */
public class AddGroupAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, AddGroupAction.class);
	private String group;

	public AddGroupAction(String[] params)
	{
		setParameters(params);
	}
	
	@Override
	public String getName()
	{
		return AddGroupActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		log.debug("Adding group membership " + group);
		Map<String, RemoteGroupMembership> groups = input.getGroups();
		RemoteGroupMembership newGr = new RemoteGroupMembership(group); 
		newGr.getMetadata().put(RemoteInformationBase.UNITY_GROUP, group);
		groups.put(group, newGr);
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {group};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 1)
			throw new IllegalArgumentException("Action requires exactely 1 parameter");
		group = parameters[0];
	}
}
