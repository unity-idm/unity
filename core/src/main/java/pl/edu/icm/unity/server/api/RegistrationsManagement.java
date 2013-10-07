/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Registrations support: forms, submissions of requests and their processing.
 * @author K. Benedyczak
 */
public interface RegistrationsManagement
{
	/**
	 * Add a new registration form.
	 * @param form
	 * @throws EngineException
	 */
	public void addForm(RegistrationForm form) throws EngineException;
	
	/**
	 * Remove an existing registration form.
	 * @param formId
	 * @param dropRequests if true then all requests of this form are deleted. If false, the operation
	 * will throw exception if there are any forms for the form.
	 * @throws EngineException
	 */
	public void removeForm(String formId, boolean dropRequests) throws EngineException;
	
	/**
	 * Updates an existing form.
	 * @param updatedForm
	 * @param ignoreRequests if true then operation will ignore form requests. If false then it will fail if there 
	 * are any pending requests of the form.
	 * @throws EngineException
	 */
	public void updateForm(RegistrationForm updatedForm, boolean ignoreRequests) throws EngineException;
	
	/**
	 * 
	 * @return all available forms.
	 * @throws EngineException
	 */
	public List<RegistrationForm> getForms() throws EngineException;
	
	/**
	 * Submits a new registration request. It gets a pending state.
	 * @param request
	 * @return automatically asigned identifier of the request
	 * @throws EngineException
	 */
	public String submitRegistrationRequest(RegistrationRequest request) throws EngineException;
	
	/**
	 * Lists all registration requests.
	 * @return
	 * @throws EngineException
	 */
	public List<RegistrationRequestState> getRegistrationRequests() throws EngineException;
	
	/**
	 * Accepts, deletes or rejects a given registration request. The request can be freely modified at this time
	 * too. 
	 * @param id request id to be processed
	 * @param finalRequest updated registration request with edits made by admin
	 * @param action what to do with the request.
	 * @param publicComment comment to be recorded and sent to the requester
	 * @param privateComment comment to be internally recored only.
	 * @throws EngineException
	 */
	public void processReqistrationRequest(String id, RegistrationRequest finalRequest, 
			RegistrationRequestAction action, AdminComment publicComment, 
			AdminComment privateComment) throws EngineException;
}
