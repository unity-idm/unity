/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;

public interface ConfirmationFacility
{
	public ConfirmationStatus confirm(String state) throws EngineException;
	public String getId();
}
