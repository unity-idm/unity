/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;



/**
 * Implementations of this interface are called before update 
 * of a referenced element.
 * @author K. Benedyczak
 */
public interface ReferenceUpdateHandler<T>
{
	/**
	 * 
	 * @param modifiedId
	 * @param modifiedName original name or null if the element has no name
	 * @param newValue 
	 */
	void preUpdateCheck(long modifiedId, String modifiedName, T newValue);
}
