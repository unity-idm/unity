/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.confirmations;

/**
 * Represent element which can be confirmed
 * @author P. Piernik
 *
 */
public interface VerifiableElement
{
	public void setVerified(boolean verified);
	public String getValue();
}
