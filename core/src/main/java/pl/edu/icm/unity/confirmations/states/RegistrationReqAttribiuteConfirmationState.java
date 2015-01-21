/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

/**
 * Contains necessary informations used during the confirmation a attribute in
 * registration request
 * 
 * @author P. Piernik
 * 
 */
public class RegistrationReqAttribiuteConfirmationState extends AttribiuteConfirmationState
{
	public static final String FACILITY_ID = "RegistrationReqAttributeFacility";
	
	public RegistrationReqAttribiuteConfirmationState(String serializedState)
	{
		super(serializedState);
	}

	public RegistrationReqAttribiuteConfirmationState(String owner,
			String type, String value, String locale, String group)
	{
		super(owner, type, value, locale, group);
	}


	@Override
	public String getFacilityId()
	{
		return FACILITY_ID;
	}
}
