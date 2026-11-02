package pl.edu.icm.unity.engine.project;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.project.ProjectAuthorizationFactory;
import pl.edu.icm.unity.engine.api.project.RestProjectAuthorization;

@Component
class ProjectAuthorizationFactoryImpl implements ProjectAuthorizationFactory
{
	private final AttributesManagement attributes;

	ProjectAuthorizationFactoryImpl(@Qualifier("insecure") AttributesManagement attributes)
	{
		this.attributes = attributes;
	}

	@Override
	public RestProjectAuthorization createRest(String projectsRootPath, String apiUsersGroupPath)
	{
		return new ProjectRestAuthorization(attributes, projectsRootPath, apiUsersGroupPath);
	}
}
