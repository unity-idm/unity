package io.imunity.upman.rest;

import javax.ws.rs.NotFoundException;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;


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
