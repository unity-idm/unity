/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class UpmanRestAuthorizationManager
{
	private final AttributesManagement attrDao;

	@Autowired
	public UpmanRestAuthorizationManager(AttributesManagement attrDao)
	{
		this.attrDao = attrDao;
	}

	@Transactional
	public void assertManagerAuthorization(String projectPath, String groupPath, String authorizationGroupPath) throws AuthorizationException
	{
		assertGroupIsUnderProject(projectPath, groupPath);
		LoginSession client = getClient();
		assertClientIsProjectManager(authorizationGroupPath, client.getEntityId());
	}

	private void assertGroupIsUnderProject(String projectPath, String childPath) throws AuthorizationException
	{
		if (!Group.isChildOrSame(childPath, projectPath))
			throw new AuthorizationException("Access is denied. Group doesn't belong to project.");
	}

	private LoginSession getClient() throws AuthorizationException
	{
		InvocationContext authnCtx = InvocationContext.getCurrent();
		LoginSession client = authnCtx.getLoginSession();

		if (client == null)
			throw new AuthorizationException("Access is denied. The client is not authenticated.");

		if (client.isUsedOutdatedCredential())
		{

			throw new AuthorizationException("Access is denied. The client's credential "
					+ "is outdated and the only allowed operation is the credential update");
		}
		return client;
	}

	private void assertClientIsProjectManager(String projectPath, long clientId) throws AuthorizationException
	{
		Set<GroupAuthorizationRole> roles = getAuthManagerAttribute(projectPath, clientId);

		if (!(roles.contains(GroupAuthorizationRole.manager)
				|| roles.contains(GroupAuthorizationRole.projectsAdmin)))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires manager capability in " + projectPath
							+ " group");
		}
	}

	private Set<GroupAuthorizationRole> getAuthManagerAttribute(String projectPath, long entity)
	{
		List<AttributeExt> attributes;
		try
		{
			attributes = new ArrayList<>(
				attrDao.getAttributes(
					new EntityParam(entity),
					projectPath,
				"sys:ProjectManagementRESTAPIRole")
			);

		} catch (Exception e)
		{
			throw new InternalException("Can not get group authorization attribute of entity " + entity);
		}

		Set<GroupAuthorizationRole> roles = new HashSet<>();
		for (AttributeExt attr : attributes)
		{
			for (String val : attr.getValues())
			{
				roles.add(GroupAuthorizationRole.valueOf(val));
			}
		}
		return roles;
	}
}
