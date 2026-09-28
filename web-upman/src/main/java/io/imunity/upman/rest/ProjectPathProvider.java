package io.imunity.upman.rest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.ws.rs.BadRequestException;

class ProjectPathProvider
{
	static String urlId(String projectId)
	{
		return projectId.contains("/") ? "~n~" + Base64.getUrlEncoder().withoutPadding()
				.encodeToString(projectId.getBytes(StandardCharsets.UTF_8)) : projectId;
	}

	static String getProjectPath(String projectId, String rootGroup)
	{
		String relative = projectId;
		if (projectId != null && projectId.startsWith("~n~"))
		{
			try
			{
				String decoded = new String(Base64.getUrlDecoder().decode(projectId.substring(3)),
						StandardCharsets.UTF_8);
				if (decoded.contains("/") && urlId(decoded).equals(projectId))
					relative = decoded;
			} catch (IllegalArgumentException e)
			{
				relative = projectId;
			}
		}
		validateRelativePath(relative);
		return (rootGroup.equals("/") ? "" : rootGroup) + "/" + relative;
	}

	static String resolveGroupPath(String projectPath, String relativePath)
	{
		if (relativePath == null || relativePath.isEmpty())
			return projectPath;
		validateRelativePath(relativePath);
		return projectPath + "/" + relativePath;
	}

	static String relativeProjectId(String projectPath, String rootGroup)
	{
		return projectPath.substring(rootGroup.equals("/") ? 1 : rootGroup.length() + 1);
	}

	static void validateRelativePath(String path)
	{
		if (path == null || path.isBlank() || path.startsWith("/") || path.endsWith("/")
				|| path.contains("\\") || path.contains("%") || path.chars().anyMatch(Character::isISOControl))
			throw new BadRequestException("Invalid relative group path");
		for (String segment : path.split("/", -1))
			if (segment.isEmpty() || segment.equals(".") || segment.equals(".."))
				throw new BadRequestException("Invalid relative group path");
	}
}
