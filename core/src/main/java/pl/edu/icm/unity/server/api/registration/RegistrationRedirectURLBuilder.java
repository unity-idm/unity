/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.registration;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Creates redirect URL which shall be used in certain situations after registration request submission 
 * or confirmation of request's email.   
 * @author K. Benedyczak
 */
public class RegistrationRedirectURLBuilder
{
	public enum Status {submitted, submittedAccepted, submittedWithError, cancelled, elementConfirmed,
		elementConfirmationError}
	public enum ConfirmedElementType {identity, attribute}
	
	public static final String PARAM_STATUS = "status";
	public static final String PARAM_ERROR_CODE = "error_code";
	public static final String PARAM_FORM_ID = "form_id";
	public static final String PARAM_REQUEST_ID = "request_id";
	public static final String PARAM_CONFIRMED_ELEMENT_TYPE = "confirmed_element_type";
	public static final String PARAM_CONFIRMED_ELEMENT_NAME = "confirmed_element_name";
	public static final String PARAM_CONFIRMED_ELEMENT_VALUE = "confirmed_element_value";
	
	private URIBuilder uriBuilder;
	private boolean noRedirect;
	
	public RegistrationRedirectURLBuilder(RegistrationForm form, String requestId, Status status)
	{
		this(form.getRedirectAfterSubmit(), form.getName(), requestId, status);
	}
	
	public RegistrationRedirectURLBuilder(String baseUrl, String formName, String requestId, Status status)
	{
		if (baseUrl == null)
		{
			noRedirect = true;
			baseUrl = "http://localhost";
		}
		try
		{
			uriBuilder = new URIBuilder(baseUrl);
		} catch (URISyntaxException e)
		{
			throw new IllegalStateException("Form has illegal redirect URI, shouldn't happen", e);
		}
		
		uriBuilder.addParameter(PARAM_STATUS, status.toString());
		if (formName != null)
			uriBuilder.addParameter(PARAM_FORM_ID, formName);
		if (requestId != null)
			uriBuilder.addParameter(PARAM_REQUEST_ID, requestId);
	}
	
	public RegistrationRedirectURLBuilder setErrorCode(String errorCode)
	{
		uriBuilder.addParameter(PARAM_ERROR_CODE, errorCode);
		return this;
	}
	
	public RegistrationRedirectURLBuilder setConfirmationInfo(ConfirmedElementType type, String name, String value)
	{
		uriBuilder.addParameter(PARAM_CONFIRMED_ELEMENT_TYPE, type.toString()).
			addParameter(PARAM_CONFIRMED_ELEMENT_NAME, name).
			addParameter(PARAM_CONFIRMED_ELEMENT_VALUE, value);
		return this;
	}
	
	@Override
	public String toString()
	{
		return noRedirect ? null : uriBuilder.toString();
	}
}