package io.imunity.upman.rest;

class ProjectPathProvider
{
	static String getProjectPath(String projectId, String rootGroup)
	{
		if(projectId.contains("/"))
			throw new IllegalArgumentException("Project Id cannot start form /");
		return rootGroup + "/" + projectId;
	}

}
