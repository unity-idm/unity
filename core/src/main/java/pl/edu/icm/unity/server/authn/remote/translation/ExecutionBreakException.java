/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Throw to signal that further execution of the translation profile should be stopped.
 * @author K. Benedyczak
 */
public class ExecutionBreakException extends EngineException
{
}
