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
public class RegistrationReqAttribiuteState extends EntityAttribiuteState
{
	public static final String FACILITY_ID = "RegistrationAttributeFacility";
	
	@Override
	public String getFacilityId()
	{
		return FACILITY_ID;
	}
}
