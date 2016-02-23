/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

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
	 * @throws EngineException
	 */
	void removeEnquiry(String formId) throws EngineException;
	
	/**
	 * Updates an existing enquiry form. Will be applicable only to those users who has not yet filled the 
	 * original enquiry.
	 * @param updatedForm
	 * @throws EngineException
	 */
	void updateEnquiry(EnquiryForm updatedForm) throws EngineException;
	
	/**
	 * 
	 * @return all available enquires.
	 * @throws EngineException
	 */
	List<EnquiryForm> getEnquires() throws EngineException;
	
	/**
	 * Submits an enquiry response. The response is encoded in {@link RegistrationRequest} class
	 * as it contains everything required in the form. Note however  
	 * @param response
	 * @throws EngineException
	 */
	void submitEnquiryResponse(EnquiryResponse response) throws EngineException;
}
