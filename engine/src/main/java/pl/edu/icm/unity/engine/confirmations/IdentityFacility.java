/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.confirmations.ConfirmationFaciliity;
import pl.edu.icm.unity.server.api.confirmations.ConfirmationStatus;


/**
 * IdentityEmail verification facility.
 * @author P. Piernik
 */
public class IdentityFacility implements ConfirmationFaciliity
{

	public static final String NAME = "identityVerificator";
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifi EmailIdentity";
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		return new ConfirmationStatus(false, "");
		
	}


}
