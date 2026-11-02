package pl.edu.icm.unity.engine.api.project;

import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

/** Endpoint-configured REST policy. Project arguments are canonical absolute group paths. */
public interface RestProjectAuthorization extends ProjectAuthorization
{
	String API_SCOPE = "sys:upman-api";
	String PROJECT_SCOPE_PREFIX = API_SCOPE + ":";
	String AUTHORIZATION_ROLE = "sys:ProjectManagementRESTAPIRole";

	void assertProjectCreationAuthorization() throws AuthorizationException;

	void assertProjectsDiscoveryAuthorization() throws AuthorizationException;
}
