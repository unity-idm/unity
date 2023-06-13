/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Signals an error of too many attempts, e.g. of password reset.
 * @author K. Benedyczak
 */
public class TooManyAttempts extends EngineException
{
}
