/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Several classes in the API must have default constructors.
 * Those classes implement this interface to offer a simple way to check if the 
 * instance is in a fully initialized state. 
 * 
 * @author K. Benedyczak
 */
public interface InitializationValidator
{
	/**
	 * Thorws unchecked exception if some of the fields are null or incomplete.
	 */
	public void validateInitialization();
}
