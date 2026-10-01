package io.imunity.upman.rest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.ws.rs.BadRequestException;

class ProjectPathProvider
{
	// A relative project ID cannot start with '/', so the decoded escape prefix cannot collide with a project name.
	private static final String ESCAPED_ID_PREFIX = "/~e~";
	private static final String ENCODED_ESCAPED_ID_PREFIX = "%2F~e~";

	static String urlId(String projectId)
	{
		if (projectId.contains("%"))
			return ENCODED_ESCAPED_ID_PREFIX + Base64.getUrlEncoder().withoutPadding()
					.encodeToString(projectId.getBytes(StandardCharsets.UTF_8));
		return URLEncoder.encode(projectId, StandardCharsets.UTF_8)
				.replace("+", "%20")
				.replace("%7E", "~");
	}

	static String getProjectPath(String projectId, String rootGroup)
	{
		return getNewProjectPath(decodeUrlId(projectId), rootGroup);
	}

	static String getNewProjectPath(String projectId, String rootGroup)
	{
		validateRelativePath(projectId);
		return (rootGroup.equals("/") ? "" : rootGroup) + "/" + projectId;
	}

	private static String decodeUrlId(String urlId)
	{
		if (urlId == null || !urlId.startsWith(ESCAPED_ID_PREFIX))
			return urlId;
		try
		{
			String encoded = urlId.substring(ESCAPED_ID_PREFIX.length());
			String decoded = new String(Base64.getUrlDecoder().decode(encoded),
					StandardCharsets.UTF_8);
			if (urlId(decoded).equals(ENCODED_ESCAPED_ID_PREFIX + encoded))
				return decoded;
		} catch (IllegalArgumentException e)
		{
			throw new BadRequestException("Invalid project URL ID", e);
		}
		throw new BadRequestException("Invalid project URL ID");
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
				|| path.contains("\\") || path.chars().anyMatch(Character::isISOControl))
			throw new BadRequestException("Invalid relative group path");
		for (String segment : path.split("/", -1))
			if (segment.isEmpty() || segment.equals(".") || segment.equals(".."))
				throw new BadRequestException("Invalid relative group path");
	}
}
