/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

/**
 * Controls how the pre filled entries are treated
 *
 * @author Krzysztof Benedyczak
 */
public enum PrefilledEntryMode {
	/**
	 * Entry is presented in the form, but becomes read only
	 */
	READ_ONLY, 
	
	/**
	 * Entry is presented with a default value
	 */
	DEFAULT, 
	
	/**
	 * Entry is hidden, so the user can neither read nor edit the pre-filled value.
	 */
	HIDDEN;
	
	public boolean isInteractivelyEntered()
	{
		return this == DEFAULT; 
	}
}