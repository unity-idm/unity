/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import pl.edu.icm.unity.types.basic.ConfirmationData;

/**
 * Represent element which can be confirmed
 * @author P. Piernik
 *
 */
public interface VerifiableElement
{
	public ConfirmationData getConfirmationData();
	public void setConfirmationData(ConfirmationData confirmationData);
	public String getValue();
}
