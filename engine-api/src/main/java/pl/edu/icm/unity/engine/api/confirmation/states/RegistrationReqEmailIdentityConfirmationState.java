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
public class RegistrationReqEmailIdentityConfirmationState extends RegistrationEmailConfirmationState
{
	public static final String FACILITY_ID = "RegistrationReqIdentityFacility";

	public RegistrationReqEmailIdentityConfirmationState(String serializedState)
	{
		super(serializedState);
	}

	public RegistrationReqEmailIdentityConfirmationState(String requestId, String type,
			String value, String locale, RequestType requestType)
	{
		super(FACILITY_ID, type, value, locale, requestId, requestType);
	}
}
