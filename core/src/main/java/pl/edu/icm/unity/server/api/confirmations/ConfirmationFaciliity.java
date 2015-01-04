/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.confirmations;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.DescribedObject;

public interface ConfirmationFaciliity extends DescribedObject
{
	public ConfirmationStatus confirm(String state) throws EngineException;
}
