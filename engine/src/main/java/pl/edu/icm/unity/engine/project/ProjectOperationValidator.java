package pl.edu.icm.unity.engine.project;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.store.api.GroupDAO;

@Component
class ProjectOperationValidator
{
	private final GroupDAO groups;

	ProjectOperationValidator(GroupDAO groups)
	{
		this.groups = groups;
	}

	void validateProject(String projectPath) throws AuthorizationException
	{
		if (!getGroup(projectPath).getDelegationConfiguration().enabled)
			throw new AuthorizationException("Access is denied. The operation requires enabled delegation on "
					+ projectPath + " group");
	}

	void validateGroup(String projectPath, String groupPath) throws AuthorizationException
	{
		validateProject(projectPath);
		validateContainment(projectPath, groupPath);
	}

	void validateSubproject(String projectPath, String groupPath) throws AuthorizationException
	{
		GroupDelegationConfiguration config = getGroup(projectPath).getDelegationConfiguration();
		if (!config.enabled || !config.enableSubprojects)
			throw new AuthorizationException("Access is denied. The operation requires enabled delegation and "
					+ "subprojects creation on " + projectPath + " group");
		validateContainment(projectPath, groupPath);
	}

	void validateRoleAssignment(String projectPath, String groupPath) throws AuthorizationException
	{
		validateGroup(projectPath, groupPath);
		if (!projectPath.equals(groupPath))
			validateProject(groupPath);
	}

	void validateContainment(String projectPath, String groupPath)
	{
		if (!Group.isChildOrSame(groupPath, projectPath))
			throw new IllegalArgumentException("Group " + groupPath
					+ " is not child of main project group " + projectPath);
	}

	private Group getGroup(String path)
	{
		try
		{
			return groups.get(path);
		} catch (Exception e)
		{
			throw new InternalException("Can not get group " + path, e);
		}
	}
}
