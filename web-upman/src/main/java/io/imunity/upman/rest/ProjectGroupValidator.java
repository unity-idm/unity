package io.imunity.upman.rest;

import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.NotFoundException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.utils.Log;

class ProjectGroupValidator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, ProjectGroupValidator.class);

	static void assertIsProjectGroup(String projectId, Group group)
	{
		if (group.getDelegationConfiguration() == null || !group.getDelegationConfiguration().enabled)
		{
			log.error("Trying to manipulate a plain group {} as it was a project", group.toString());
			throw new NotFoundException("There is no project " + projectId);
		}
	}
}
