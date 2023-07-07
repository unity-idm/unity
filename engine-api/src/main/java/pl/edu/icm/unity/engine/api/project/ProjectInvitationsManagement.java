/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Internal engine API for project invitations management
 */
public interface ProjectInvitationsManagement
{
	void addInvitations(Set<ProjectInvitationParam> param) throws EngineException;

	/**
	 * Gets all project invitations
	 */
	List<ProjectInvitation> getInvitations(String projectPath) throws EngineException;

	/**
	 * Removes a single invitation
	 */
	void removeInvitation(String projectPath, String code) throws EngineException;

	/**
	 * Sends an invitation message to the invitation specified by the code.
	 * In case when there is no such invitation, it has missing or invalid
	 * contact address or when the associated form has no message template
	 * for invitation this method throws exception.
	 */
	void sendInvitation(String projectPath, String code) throws EngineException;
	

	public static class NotProjectInvitation extends InternalException
	{
		public NotProjectInvitation(String projectPath, String code)
		{
			super("Invitation with code " + code + " is not related with project group " + projectPath);
		}
	}

	public static class IllegalInvitationException extends InternalException
	{
		public IllegalInvitationException(String code)
		{
			super("Invitation with code " + code + " does not exists");
		}
	}
	
	public static class ProjectMisconfiguredException extends InternalException
	{
		public ProjectMisconfiguredException(String projectPath)
		{
			super("Misconfigured project group " + projectPath);
		}
	}
	
	public static class AlreadyMemberException extends RuntimeException
	{
	}
}
