package pl.edu.icm.unity.engine.api.project;

public interface ProjectAuthorizationFactory
{
	RestProjectAuthorization createRest(String projectsRootPath, String apiUsersGroupPath);
}
