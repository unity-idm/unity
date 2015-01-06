/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;


/**
 * Contains necessary informations used during the confirmation a identity in
 * registration request
 * 
 * @author P. Piernik
 * 
 */
public class IdentityFromRegState extends IdentityConfirmationState
{

	public static final String FACILITY_ID = "identityFromRegistrationFacility";

	@Override
	public String getFacilityId()
	{
		return FACILITY_ID;
	}
}
