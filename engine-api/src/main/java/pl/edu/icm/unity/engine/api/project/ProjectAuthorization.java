package pl.edu.icm.unity.engine.api.project;

import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

public interface ProjectAuthorization
{
	void assertManagedProjectsListingAuthorization() throws AuthorizationException;

	/** Filters discovery results after the caller has passed the applicable listing authorization check. */
	boolean isProjectVisible(String projectPath);

	void assertManagerAuthorization(String projectPath) throws AuthorizationException;

	void assertProjectsAdminAuthorization(String projectPath) throws AuthorizationException;

	void assertRoleManagerAuthorization(String projectPath, String groupPath, GroupAuthorizationRole role)
			throws AuthorizationException;
}
