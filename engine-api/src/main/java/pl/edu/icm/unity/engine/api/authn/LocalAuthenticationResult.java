/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.MessageSource;

/**
 * This class object is returned by local authenticator with information about authentication result. 
 */
public class LocalAuthenticationResult implements AuthenticationResult
{
	private Status status;	
	private final SuccessResult successResult;
	private final NotApplicableResult notApplicableResult;
	private final ErrorResult errorResult;

	private LocalAuthenticationResult(Status status, 
			SuccessResult successResult,
			NotApplicableResult notApplicableResult, 
			ErrorResult errorResult)
	{
		this.status = status;
		this.successResult = successResult;
		this.notApplicableResult = notApplicableResult;
		this.errorResult = errorResult;
	}


	public static LocalAuthenticationResult failed()
	{
		return failed(ResolvableError.EMPTY, null);
	}
	
	public static LocalAuthenticationResult failed(Exception cause)
	{
		return failed(ResolvableError.EMPTY, cause);
	}

	public static LocalAuthenticationResult failed(ResolvableError error)
	{
		return failed(error, null);
	}
	
	public static LocalAuthenticationResult failed(ResolvableError error, Exception cause)
	{
		return new LocalAuthenticationResult(Status.deny, null, null, new ErrorResult(error, cause));
	}
	
	public static LocalAuthenticationResult notApplicable()
	{
		return new LocalAuthenticationResult(Status.notApplicable, null, new NotApplicableResult(), null);
	}

	public static LocalAuthenticationResult successful(AuthenticatedEntity authenticatedEntity)
	{
		return new LocalAuthenticationResult(Status.success, new SuccessResult(authenticatedEntity), null, null);
	}

	@Override
	public boolean isRemote()
	{
		return false;
	}
	
	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public SuccessResult getSuccessResult()
	{
		if (status != Status.success)
			throw new IllegalStateException("Can be aonly called on successful result, but we are " + status);
		return successResult;
	}

	public NotApplicableResult getNotApplicableResult()
	{
		if (status != Status.notApplicable)
			throw new IllegalStateException("Can be aonly called on notApplicable result, but we are " + status);
		return notApplicableResult;
	}

	@Override
	public ErrorResult getErrorResult()
	{
		if (status != Status.deny)
			throw new IllegalStateException("Can be aonly called on deny result, but we are " + status);
		return errorResult;
	}

	@Override
	public String toStringFull()
	{
		Object detail = successResult != null ? successResult : 
			notApplicableResult != null ? notApplicableResult : errorResult;
		return "AuthenticationResult: \nstatus=" + status + "\ndetails=" + detail;
	}

	@Override
	public String toString()
	{
		return status.toString();
	}
	
	public static class ErrorResult 
	{
		public final ResolvableError error;
		public final Exception cause;
		
		ErrorResult(ResolvableError error, Exception cause)
		{
			this.error = error;
			this.cause = cause;
		}

		@Override
		public String toString()
		{
			return String.format("ErrorResult [error=%s, cause=%s]", error, cause);
		}
	}
	
	public static class SuccessResult 
	{
		public final AuthenticatedEntity authenticatedEntity;

		SuccessResult(AuthenticatedEntity authenticatedEntity)
		{
			this.authenticatedEntity = authenticatedEntity;
		}

		@Override
		public String toString()
		{
			return String.format("[authenticatedEntity=%s]", authenticatedEntity);
		}
	}
	
	public static class NotApplicableResult 
	{
		@Override
		public String toString()
		{
			return "-not applicable-";
		}
	}
	
	public static class ResolvableError
	{
		static final ResolvableError EMPTY = new ResolvableError(null);
		private final String errorCode;
		private final Object[] args;
		
		public ResolvableError(String errorCode, Object... args)
		{
			this.errorCode = errorCode;
			this.args = args;
		}
		
		public String resovle(MessageSource msg)
		{
			return errorCode == null ? null : msg.getMessage(errorCode, args);
		}
	}
}
