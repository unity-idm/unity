/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.verifiers;

import pl.edu.icm.unity.types.DescribedObject;
/**
 * Implementations providing user email verification
 * @author P. Piernik
 *
 */
public interface EmailVerificator extends DescribedObject
{
	public boolean verify(ConfirmationState state);
}
