/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
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
	void addForm(RegistrationForm form) throws EngineException;
	
	/**
	 * Remove an existing registration form.
	 * @param formId
	 * @param dropRequests if true then all requests of this form are deleted. If false, the operation
	 * will throw exception if there are any forms for the form.
	 * @throws EngineException
	 */
	void removeForm(String formId, boolean dropRequests) throws EngineException;
	
	/**
	 * Updates an existing form.
	 * 
	 * @param updatedForm
	 * @param ignoreRequestsAndInvitations
	 *                if true then operation will ignore form requests and
	 *                invitations. If false then it will fail if there are
	 *                any pending requests of the form.
	 * @throws EngineException
	 */
	void updateForm(RegistrationForm updatedForm, boolean ignoreRequestsAndInvitations) throws EngineException;

	/**
	 * 
	 * @return all available forms.
	 * @throws EngineException
	 */
	List<RegistrationForm> getForms() throws EngineException;
	
	
	/**
	 * 
	 * @return form with given id.
	 * @throws EngineException
	 */
	RegistrationForm getForm(String id) throws EngineException;

	/**
	 * @return true if form with given name exists
	 */
	boolean hasForm(String id);
	
	/**
	 * Submits a new registration request. It gets a pending state unless automatically processed by the 
	 * form's automation.
	 * Note that the input parameter can be modified by the invocation: all the supplied credential secrets
	 * are transformed to the internal (typically hashed) form.
	 * Important: this API call requires high authZ privileges. This is because the operation trusts confirmation
	 * state of the passed arguments. In case such operation shall be exposed with a public, unprivileged API,
	 * then we need an other variant, forcing unconfirmed state of parameters. This can be problematic for mobile 
	 * numbers.
	 *    
	 * @param request
	 * @param context
	 * @return automatically assigned identifier of the request
	 * @throws EngineException
	 */
	String submitRegistrationRequest(RegistrationRequest request, RegistrationContext context) 
			throws EngineException;
	
	/**
	 * Lists all registration requests.
	 * @return
	 * @throws EngineException
	 */
	List<RegistrationRequestState> getRegistrationRequests() throws EngineException;

	/**
	 * @return registration request by id
	 */
	RegistrationRequestState getRegistrationRequest(String id) throws EngineException;
	
	/**
	 * Accepts, deletes or rejects a given registration request. The request can be freely modified at this time
	 * too, with one exception: the credentials originally submitted are always preserved.
	 * @param id request id to be processed
	 * @param finalRequest updated registration request with edits made by admin
	 * @param action what to do with the request.
	 * @param publicComment comment to be recorded and sent to the requester
	 * @param privateComment comment to be internally recored only.
	 * @throws EngineException
	 */
	void processRegistrationRequest(String id, RegistrationRequest finalRequest, 
			RegistrationRequestAction action, String publicComment, 
			String privateComment) throws EngineException;
	
	
	/**
	 * @return form automation support for a given form
	 */
	FormAutomationSupport getFormAutomationSupport(RegistrationForm form);
}
