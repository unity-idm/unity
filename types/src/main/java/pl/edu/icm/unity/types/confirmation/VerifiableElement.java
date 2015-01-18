/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.confirmation;


/**
 * Represent element which can be confirmed. 
 * @author P. Piernik
 *
 */
public interface VerifiableElement
{
	public ConfirmationInfo getConfirmationInfo();
	public void setConfirmationInfo(ConfirmationInfo confirmationInfo);
	public String getValue();
}
