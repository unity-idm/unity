/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;

/**
 * Creates redirect URL which shall be used in certain situations after registration request submission 
 * or confirmation of request's email.   
 * @author K. Benedyczak
 */
public class RegistrationRedirectURLBuilder extends EmailConfirmationRedirectURLBuilder
{
	public enum Status {submitted, submittedAccepted, submittedWithError, userExists, cancelled, elementConfirmed,
		elementConfirmationError}
	
	public static final String PARAM_FORM_ID = "form_id";
	public static final String PARAM_REQUEST_ID = "request_id";
	
	public RegistrationRedirectURLBuilder(String baseUrl, String formName, String requestId, Status status)
	{
		this(baseUrl, formName, requestId, status.toString());
	}

	public RegistrationRedirectURLBuilder(String baseUrl, String formName, String requestId, TriggeringState status)
	{
		this(baseUrl, formName, requestId, status.toURLState());
	}

	private RegistrationRedirectURLBuilder(String baseUrl, String formName, String requestId, String status)
	{
		super(baseUrl, status);
		
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