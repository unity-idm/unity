/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;



/**
 * Implementations of this interface are called before removal 
 * of a referenced element.
 * @author K. Benedyczak
 */
public interface ReferenceRemovalHandler
{
	/**
	 * @param removedId removed element key
	 * @param removedName removed name or null if element has no name
	 */
	void preRemoveCheck(long removedId, String removedName);
}
