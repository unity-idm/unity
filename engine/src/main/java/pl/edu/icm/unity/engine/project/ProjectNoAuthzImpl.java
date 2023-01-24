/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;

@Component("projectNoAuthz")
@Qualifier("insecure")
public class ProjectNoAuthzImpl extends ProjectAuthorizationManager
{
	@Autowired
	public ProjectNoAuthzImpl(GroupDAO groupDao, AttributeDAO attrDao)
	{
		super(groupDao, attrDao);
	}

	@Override
	public void assertManagerAuthorization(String projectPath) throws AuthorizationException
	{
	}

	@Override
	public void assertManagerAuthorization(String projectPath, String groupPath) throws AuthorizationException
	{
	}

	@Override
	public void assertProjectsAdminAuthorization(String projectPath, String groupPath)
	{
	}

	@Override
	public void assertRoleManagerAuthorization(String projectPath, String groupPath, GroupAuthorizationRole role)
	{
	}

}
