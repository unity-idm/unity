/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.registration;

import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Creates redirect URL which shall be used in certain situations after registration request submission 
 * or confirmation of request's email.   
 * @author K. Benedyczak
 */
public class RegistrationRedirectURLBuilder extends ConfirmationRedirectURLBuilder
{
	public enum Status {submitted, submittedAccepted, submittedWithError, cancelled, elementConfirmed,
		elementConfirmationError}
	
	public static final String PARAM_FORM_ID = "form_id";
	public static final String PARAM_REQUEST_ID = "request_id";
	
	public RegistrationRedirectURLBuilder(RegistrationForm form, String requestId, Status status)
	{
		this(form.getRedirectAfterSubmit(), form.getName(), requestId, status);
	}
	
	public RegistrationRedirectURLBuilder(String baseUrl, String formName, String requestId, Status status)
	{
		super(baseUrl, status.toString());
		
		if (formName != null)
			uriBuilder.addParameter(PARAM_FORM_ID, formName);
		if (requestId != null)
			uriBuilder.addParameter(PARAM_REQUEST_ID, requestId);
	}
	
	@Override
	public String build()
	{
		return noRedirect ? null : uriBuilder.toString();
	}
}