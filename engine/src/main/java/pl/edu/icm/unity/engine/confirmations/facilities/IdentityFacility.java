/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.exceptions.EngineException;


/**
 * Identity verification facility.
 * 
 * @author P. Piernik
 */
public class IdentityFacility implements ConfirmationFacility
{
	@Override
	public String getId()
	{
		return IdentityConfirmationState.FACILITY_ID;
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		return new ConfirmationStatus(false, "");
		
	}


}
