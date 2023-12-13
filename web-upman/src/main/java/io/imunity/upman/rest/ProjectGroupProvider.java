package io.imunity.upman.rest;

import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.NotFoundException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;


class ProjectGroupProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, ProjectGroupValidator.class);
	
	private final GroupsManagement groupMan;

	ProjectGroupProvider(GroupsManagement groupMan)
	{
		this.groupMan = groupMan;
	}

	Group getProjectGroup(String projectId, String projectPath) throws EngineException
	{
		Group group = null;
		try
		{
			group = groupMan.getContents(projectPath, GroupContents.METADATA)
					.getGroup();
		} catch (GroupNotFoundException e)
		{
			log.error("Can not get project group " + projectPath , e);
			throw new NotFoundException("There is no project " + projectId);
		}

		ProjectGroupValidator.assertIsProjectGroup(projectId, group);
		return group;
	}
}
