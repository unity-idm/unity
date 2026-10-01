package pl.edu.icm.unity.engine.api.project;

import pl.edu.icm.unity.base.exceptions.EngineException;

public interface ProjectRestManagementAdapter
{
	@FunctionalInterface
	interface Operation<T>
	{
		T run() throws EngineException;
	}

	<T> T withRestManagement(String projectPath, Operation<T> operation) throws EngineException;
}
