/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;


/**
 * Contains configuration of a particular {@link LocalAuthnMethod}.
 * @author K. Benedyczak
 */
public interface LocalAuthnMethodConfiguration extends JsonSerializable
{
	/**
	 * @return to what {@link LocalAuthnMethod} this object provides configuration
	 */
	public LocalAuthnMethod getLam();
}
