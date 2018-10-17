/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.session;

/**
 * Signals that additional authentication is required prior to invoking the operation but it is not 
 * configured properly so the operation can't succeed.
 * 
 * @author K. Benedyczak
 */
public class AdditionalAuthenticationMisconfiguredException extends RuntimeException
{
}