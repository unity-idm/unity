package io.imunity.upman.rest;

import pl.edu.icm.unity.exceptions.RuntimeEngineException;

class ProjectFormValidationException extends RuntimeEngineException
{
	ProjectFormValidationException(String message)
	{
		super(message);
	}
}
