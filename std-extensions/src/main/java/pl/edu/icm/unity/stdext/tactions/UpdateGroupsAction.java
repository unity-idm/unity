/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.GroupUtils;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Adds a previously mapped identity to all groups from the context. Might add only to selected groups.
 *   
 * @author K. Benedyczak
 */
public class UpdateGroupsAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, UpdateGroupsAction.class);
	private Pattern selection;
	private GroupsManagement groupsMan;
	private IdentitiesManagement idsMan;
		
	public UpdateGroupsAction(String[] parameters, GroupsManagement groupsMan, IdentitiesManagement idsMan)
	{
		setParameters(parameters);
		this.groupsMan = groupsMan;
		this.idsMan = idsMan;
	}
	
	@Override
	public String getName()
	{
		return UpdateGroupsActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		Map<String, RemoteGroupMembership> groups = input.getGroups();
		RemoteIdentity ri = input.getPrimaryIdentity();
		if (ri == null)
		{
			log.debug("No identity, skipping");
			return;
		}
		String unityIdentity = ri.getMetadata().get(RemoteInformationBase.UNITY_IDENTITY);
		if (unityIdentity == null)
		{
			log.debug("No mapped identity, skipping");
			return;
		}
		EntityParam entity = new EntityParam(new IdentityTaV(ri.getIdentityType(), ri.getName()));
		Collection<String> currentGroups = idsMan.getGroups(entity);
		for (Map.Entry<String, RemoteGroupMembership> gm: groups.entrySet())
		{
			String group = gm.getValue().getMetadata().get(RemoteInformationBase.UNITY_GROUP);
			if (group == null)
				continue;
			if (!selection.matcher(group).matches())
				continue;

			if (!currentGroups.contains(group))
			{
				Deque<String> missingGroups = GroupUtils.getMissingGroups(group, currentGroups);
				log.info("Adding to group " + group);
				addToGroupRecursive(entity, missingGroups);
			} else
			{
				log.debug("Entity already in the group " + group + ", skipping");
			}
		}
	}

	private void addToGroupRecursive(EntityParam who, Deque<String> missingGroups) throws EngineException
	{
		String group = missingGroups.pollLast();
		groupsMan.addMemberFromParent(group, who);
		if (!missingGroups.isEmpty())
			addToGroupRecursive(who, missingGroups);
	}
	
	@Override
	public String[] getParameters()
	{
		return new String[] {selection.pattern()};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 1)
			throw new IllegalArgumentException("Action requires exactely 1 parameter");
		selection = Pattern.compile(parameters[0]);
	}
}
