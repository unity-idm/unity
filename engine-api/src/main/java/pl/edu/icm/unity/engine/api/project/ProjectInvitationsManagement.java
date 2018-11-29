/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Internal engine API for project invitations management
 * 
 * @author P.Piernik
 *
 */

public interface ProjectInvitationsManagement
{
	/**
	 * Ads invitation
	 * 
	 * @param param
	 * @return 
	 * @throws EngineException
	 */
	String addInvitation(ProjectInvitationParam param) throws EngineException;

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
	 * Sends an invitation message to the invitation specified by the code.
	 * In case when there is no such invitation, it has missing or invalid
	 * contact address or when the associated form has no message template
	 * for invitation this method throws exception.
	 * 
	 * @param code
	 */
	void sendInvitation(String projectPath, String code) throws EngineException;
}
