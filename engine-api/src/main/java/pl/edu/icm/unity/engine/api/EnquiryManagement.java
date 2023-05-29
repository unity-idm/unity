/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;

import java.util.List;
import java.util.Optional;

/**
 * Enquires support: forms, submissions of requests and their processing.
 * @author K. Benedyczak
 */
public interface EnquiryManagement
{
	/**
	 * Add a new enquiry form.
	 * @param form
	 * @throws EngineException
	 */
	void addEnquiry(EnquiryForm form) throws EngineException;
	
	/**
	 * Triggers a (re?)send of enquiry notification message.
	 * The message will be send only for those who has not yet filled the enquiry.
	 *
	 * @param enquiryId
	 * @throws EngineException
	 */
	void sendEnquiry(String enquiryId) throws EngineException;
	
	/**
	 * Remove an existing enquiry form.
 	 * @param formId
	 * @param dropRequests if true then all requests of this form are deleted. If false, the operation
	 * will throw exception if there are any requests for the form.
	 * @throws EngineException
	 */
	void removeEnquiry(String formId, boolean dropRequests) throws EngineException;
	
	/**
	 * Updates an existing enquiry form. Will be applicable only to those users who has not yet filled the 
	 * original enquiry.
	 * @param updatedForm
	 * @param ignoreRequestsAndInvitations
	 *                if true then operation will ignore form requests and
	 *                invitations. If false then it will fail if there are
	 *                any pending requests of the form.
	 * @throws EngineException
	 */
	void updateEnquiry(EnquiryForm updatedForm, boolean ignoreRequestsAndInvitations) throws EngineException;
	
	/**
	 * Accepts, deletes or rejects a given enquiry response. The request can be freely modified at this time
	 * too, with one exception: the credentials originally submitted are always preserved.
	 * @param id request id to be processed
	 * @param finalRequest updated request with edits made by admin
	 * @param action what to do with the request.
	 * @param publicComment comment to be recorded and sent to the requester
	 * @param privateComment comment to be internally recored only.
	 * @throws EngineException
	 */
	void processEnquiryResponse(String id, EnquiryResponse finalResponse, 
			RegistrationRequestAction action, String publicComment, 
			String privateComment) throws EngineException;
	
	/**
	 * @return all available enquires.
	 */
	List<EnquiryForm> getEnquires() throws EngineException;
	
	/**
	 * @return enquiry form with given id.
	 */
	EnquiryForm getEnquiry(String id) throws EngineException;

	Optional<EnquiryForm> getEnquiryByName(String name) throws EngineException;

	boolean hasForm(String id);
	
	/**
	 * Marks an enquiry as ignored for the given user. This is only possible for enquires 
	 * which are not mandatory to be filled.
	 */
	void ignoreEnquiry(String enquiryId, EntityParam entity) throws EngineException;
	
	/**
	 * Submits an enquiry response.
	 */
	String submitEnquiryResponse(EnquiryResponse response, RegistrationContext context) throws EngineException;

	/**
	 * Lists all responses
	 */
	List<EnquiryResponseState> getEnquiryResponses() throws EngineException;

	/**
	 * @return a specific enquiry response
	 */
	EnquiryResponseState getEnquiryResponse(String requestId);

	
	/**
	 * @return form automation support for a given form
	 */
	FormAutomationSupport getFormAutomationSupport(EnquiryForm form);

	
	void removePendingStickyRequest(String form, EntityParam entity) throws EngineException;

	/**
	 * Remove an existing enquiry form with no dependency checking
	 */
	void removeEnquiryWithoutDependencyChecking(String formId) throws EngineException;

	/**
	 * Allows to get enquiries according to the given filter. 
	 * @param selector filter what should be retrieved
	 */
	List<EnquiryForm> getAvailableEnquires(EntityParam entityParam, EnquirySelector selector) throws EngineException;

}
