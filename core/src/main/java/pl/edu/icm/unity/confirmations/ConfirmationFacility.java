/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.DescribedObject;

/**
 * Implementations are providing confirmation functionality.
 *
 * @author P. Piernik
 *
 */
public interface ConfirmationFacility extends DescribedObject
{
	public ConfirmationStatus confirm(String state) throws EngineException;
}
