/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

/**
 * Creates redirect URL which shall be used in certain situations after confirmation of email.   
 * @author K. Benedyczak
 */
public class EmailConfirmationRedirectURLBuilder
{
	public enum Status {elementConfirmed, elementConfirmationError}
	public enum ConfirmedElementType {identity, attribute}
	
	public static final String PARAM_STATUS = "status";
	public static final String PARAM_ERROR_CODE = "error_code";
	public static final String PARAM_CONFIRMED_ELEMENT_TYPE = "confirmed_element_type";
	public static final String PARAM_CONFIRMED_ELEMENT_NAME = "confirmed_element_name";
	public static final String PARAM_CONFIRMED_ELEMENT_VALUE = "confirmed_element_value";
	
	protected URIBuilder uriBuilder;
	protected boolean noRedirect;

	public EmailConfirmationRedirectURLBuilder(String baseUrl, Status status)
	{
		this(baseUrl, status.toString());
	}
	
	protected EmailConfirmationRedirectURLBuilder(String baseUrl, String status)
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
		
		uriBuilder.addParameter(PARAM_STATUS, status);
	}
	
	public EmailConfirmationRedirectURLBuilder setErrorCode(String errorCode)
	{
		uriBuilder.addParameter(PARAM_ERROR_CODE, errorCode);
		return this;
	}
	
	public EmailConfirmationRedirectURLBuilder setConfirmationInfo(ConfirmedElementType type, String name, String value)
	{
		uriBuilder.addParameter(PARAM_CONFIRMED_ELEMENT_TYPE, type.toString()).
			addParameter(PARAM_CONFIRMED_ELEMENT_NAME, name).
			addParameter(PARAM_CONFIRMED_ELEMENT_VALUE, value);
		return this;
	}
	
	public String build()
	{
		return noRedirect ? null : uriBuilder.toString();
	}
}