/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Base interface for specifying local authentication secret (e.g. a password)
 * 
 * @author K. Benedyczak
 */
public interface AuthenticationSecret extends JsonSerializable
{
	/**
	 * @return an id of {@link LocalAuthnVerification} for which this secret is.
	 */
	public String getLAVId();
}
