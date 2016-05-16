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
	ConfirmationInfo getConfirmationInfo();
	void setConfirmationInfo(ConfirmationInfo confirmationInfo);
	String getValue();
	/**
	 * Shorter version of getConfirmationInfo().isConfirmed(), also checking if the CI is not null.
	 * @return true if element is properly verified
	 */
	boolean isConfirmed();
}
