package io.imunity.upman.rest;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.engine.api.GroupsManagement;


class ProjectGroupProvider
{
	private final GroupsManagement groupMan;

	ProjectGroupProvider(GroupsManagement groupMan)
	{
		this.groupMan = groupMan;
	}

	Group getProjectGroup(String projectId, String projectPath) throws EngineException
	{
		Group group = groupMan.getContents(projectPath, GroupContents.METADATA)
				.getGroup();
		ProjectGroupValidator.assertIsProjectGroup(projectId, group);
		return group;
	}
}
