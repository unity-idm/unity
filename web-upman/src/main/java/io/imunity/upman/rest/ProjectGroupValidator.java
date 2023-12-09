package io.imunity.upman.rest;

import javax.ws.rs.NotFoundException;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.types.basic.Group;


class ProjectGroupValidator
{	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, ProjectGroupValidator.class);

	
	static void assertIsProjectGroup(String projectId, Group group)
	{
		if (group.getDelegationConfiguration() == null || !group.getDelegationConfiguration().enabled)
		{
			log.error("Trying to manipulate a plain group {} as it was a project", group.toString());
			throw new NotFoundException("There is no project " + projectId);
		}
	}
}
