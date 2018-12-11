/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.List;
import java.util.Optional;

import pl.edu.icm.unity.engine.api.project.ProjectRequest.RequestOperation;
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
	 * 
	 * @param project
	 * @return
	 */
	List<ProjectRequest> getRequests(String projectPath) throws EngineException;

	/**
	 * 
	 * @param id
	 */
	void accept(String projectPath, String id, RequestOperation operation) throws EngineException;

	/**
	 * 
	 * @param id
	 */
	void decline(String projectPath, String id, RequestOperation operation) throws EngineException;
	
	
	/**
	 * 
	 * @param projectPath
	 * @return
	 * @throws EngineException
	 */
	Optional<String> getProjectRegistrationFormLink(String projectPath) throws EngineException;

}
