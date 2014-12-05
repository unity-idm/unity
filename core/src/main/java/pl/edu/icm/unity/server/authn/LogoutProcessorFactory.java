/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

/**
 * Factory of {@link LogoutProcessor}s.
 * @author K. Benedyczak
 */
public interface LogoutProcessorFactory
{
	LogoutProcessor getInstance();
}