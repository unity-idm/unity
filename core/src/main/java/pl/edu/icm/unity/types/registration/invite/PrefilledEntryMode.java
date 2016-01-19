/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
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
	HIDDEN
}