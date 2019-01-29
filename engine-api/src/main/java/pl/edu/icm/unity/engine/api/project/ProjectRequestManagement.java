/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.List;
import java.util.Optional;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Engine API for project update request management
 * 
 * @author P.Piernik
 *
 */

public interface ProjectRequestManagement
{
	/**
	 * Get all project requests
	 * @param project
	 * @return
	 */
	List<ProjectRequest> getRequests(String projectPath) throws EngineException;

	/**
	 * Accept project request
	 * @param id
	 */
	void accept(ProjectRequestParam request) throws EngineException;

	/**
	 * Reject project request
	 * @param id
	 */
	void decline(ProjectRequestParam request) throws EngineException;
	
	Optional<String> getProjectRegistrationFormLink(String projectPath) throws EngineException;

	Optional<String> getProjectSignUpEnquiryFormLink(String projectPath) throws EngineException;
	
	Optional<String> getProjectUpdateMembershipEnquiryFormLink(String projectPath) throws EngineException;

}
