/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

/**
 * Interface implemented by modules providing server initialization logic, e.g. loading default contents.
 * @author K. Benedyczak
 */
public interface ServerInitializer extends Runnable
{
	public String getName();
}
