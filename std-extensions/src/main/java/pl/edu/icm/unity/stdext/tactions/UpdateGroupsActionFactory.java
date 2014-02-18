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
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Factory for {@link UpdateGroupsAction}
 *   
 * @author K. Benedyczak
 */
@Component
public class UpdateGroupsActionFactory implements TranslationActionFactory
{
	public static final String NAME = "updateGroups";
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private IdentitiesManagement idsMan;

	@Autowired
	public UpdateGroupsActionFactory(@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") IdentitiesManagement idsMan, UnityMessageSource msg)
	{
		this.msg = msg;
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
		return msg.getMessage("TranslationAction.updateGroups.desc");
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return new ActionParameterDesc[] { new ActionParameterDesc(true,
				msg.getMessage("TranslationAction.updateGroups.param.1.name"),
				msg.getMessage("TranslationAction.updateGroups.param.1.desc"), 20) };
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new UpdateGroupsAction(parameters, groupsMan, idsMan);
	}
}
