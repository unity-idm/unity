/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation.states;

/**
 * Contains necessary informations used during the confirmation a identity in
 * registration request
 * 
 * @author P. Piernik
 * 
 */
public class RegistrationReqIdentityConfirmationState extends RegistrationConfirmationState
{
	public static final String FACILITY_ID = "RegistrationReqIdentityFacility";

	public RegistrationReqIdentityConfirmationState(String serializedState)
	{
		super(serializedState);
	}

	public RegistrationReqIdentityConfirmationState(String requestId, String type,
			String value, String locale, String redirectUrl, RequestType requestType)
	{
		super(FACILITY_ID, type, value, locale, redirectUrl, requestId, requestType);
	}
	
	public RegistrationReqIdentityConfirmationState(String requestId, String type,
			String value, String locale, RequestType requestType)
	{
		super(FACILITY_ID, type, value, locale, requestId, requestType);
	}
}
