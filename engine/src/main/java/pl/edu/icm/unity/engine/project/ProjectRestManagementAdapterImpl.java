package pl.edu.icm.unity.engine.project;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.project.ProjectRestManagementAdapter;

@Component
class ProjectRestManagementAdapterImpl implements ProjectRestManagementAdapter
{
	private final ProjectAuthorizationManager authorization;

	ProjectRestManagementAdapterImpl(ProjectAuthorizationManager authorization)
	{
		this.authorization = authorization;
	}

	@Override
	public <T> T withRestManagement(String projectPath, Operation<T> operation) throws EngineException
	{
		return authorization.withRestManagement(projectPath, operation::run);
	}
}
