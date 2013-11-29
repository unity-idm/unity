/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;

/**
 * Factory for {@link UpdateGroupsAction}
 *   
 * @author K. Benedyczak
 */
@Component
public class UpdateGroupsActionFactory implements TranslationActionFactory
{
	public static final String NAME = "updateGroups";
	
	private static final ActionParameterDesc[] PARAMS = {
		new ActionParameterDesc(true, "groups", 
				"Regular expression selecting the groups to which the client should be added.", 20)
	};
	
	private GroupsManagement groupsMan;
	private IdentitiesManagement idsMan;

	@Autowired
	public UpdateGroupsActionFactory(@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") IdentitiesManagement idsMan)
	{
		this.groupsMan = groupsMan;
		this.idsMan = idsMan;
	}
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Adds the client to the groups which were provided by the remote IdP. Only groups that " +
				"has been previously mapped to the unity group are considered (other are ignored)." +
				" A subset of all groups can be selected with parameter.";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return PARAMS;
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new UpdateGroupsAction(parameters, groupsMan, idsMan);
	}
}
