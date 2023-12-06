package pl.edu.icm.unity.engine.api.translation;

import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;

public class ActionValidationException extends RuntimeEngineException
{
	public ActionValidationException(String message)
	{
		super(message);
	}
}
