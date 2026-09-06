/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;

/**
 * Internal engine API for project invitations management
 * 
 * @author P.Piernik
 *
 */

public interface ProjectInvitationsManagement
{
	/**
	 * Ads invitations
	 * 
	 * @param param
	 * @return 
	 * @throws EngineException
	 */
	ProjectAddInvitationResult addInvitations(Set<ProjectInvitationParam> param) throws EngineException;

	/**
	 * Gets all project invitations
	 * 
	 * @param projectPath
	 * @return
	 * @throws EngineException
	 */
	List<ProjectInvitation> getInvitations(String projectPath) throws EngineException;

	/**
	 * Removes a single invitation
	 * 
	 * @param code
	 * @throws EngineException
	 */
	void removeInvitation(String projectPath, String code) throws EngineException;

	/**
	 * Sends the existing invitation message again. The invitation must remain valid for at least
	 * {@link ProjectInvitation#MINIMUM_RESEND_VALIDITY}.
	 * In case when there is no such invitation, it has missing or invalid
	 * contact address or when the associated form has no message template
	 * for invitation this method throws exception.
	 *
	 * @param code
	 */
	void resendInvitation(String projectPath, String code) throws EngineException;

	/**
	 * Creates and sends a new invitation based on the invitation specified by the code, and removes the old invitation.
	 * The new invitation has a new code and its validity period starts when this method is called.
	 *
	 * @param code
	 */
	void reinvite(String projectPath, String code) throws EngineException;
	

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
}
