/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.Optional;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;

/**
 * This class object is returned by local authenticator with information about authentication result. 
 */
public class LocalAuthenticationResult implements AuthenticationResult
{
	private final Status status;	
	private final SuccessResult successResult;
	private final NotApplicableResult notApplicableResult;
	private final ErrorResult errorResult;
	private final DenyReason denyReason;

	private LocalAuthenticationResult(Status status, 
			SuccessResult successResult,
			NotApplicableResult notApplicableResult, 
			ErrorResult errorResult,
			DenyReason denyReason)
	{
		this.status = status;
		this.successResult = successResult;
		this.notApplicableResult = notApplicableResult;
		this.errorResult = errorResult;
		this.denyReason = denyReason;
	}


	public static LocalAuthenticationResult failed()
	{
		return failed(ResolvableError.EMPTY, null, null);
	}
	
	public static LocalAuthenticationResult failed(Exception cause)
	{
		return failed(ResolvableError.EMPTY, cause);
	}
	
	public static LocalAuthenticationResult failed(Exception cause, DenyReason denyReason)
	{
		return failed(ResolvableError.EMPTY, cause, denyReason);
	}

	public static LocalAuthenticationResult failed(ResolvableError error)
	{
		return failed(error, null, null);
	}
	
	public static LocalAuthenticationResult failed(ResolvableError error, DenyReason denyReason)
	{
		return failed(error, null, denyReason);
	}
	
	public static LocalAuthenticationResult failed(ResolvableError error, Exception cause, DenyReason denyReason)
	{
		return new LocalAuthenticationResult(Status.deny, null, null, new ErrorResult(error, cause), denyReason);
	}
	
	public static LocalAuthenticationResult failed(ResolvableError error, Exception cause)
	{
		return new LocalAuthenticationResult(Status.deny, null, null, new ErrorResult(error, cause), null);
	}
	
	public static LocalAuthenticationResult notApplicable()
	{
		return new LocalAuthenticationResult(Status.notApplicable, null, new NotApplicableResult(), null, null);
	}

	public static LocalAuthenticationResult successful(AuthenticatedEntity authenticatedEntity, AuthenticationMethod authenticationMethod)
	{
		return new LocalAuthenticationResult(Status.success, new SuccessResult(authenticatedEntity, authenticationMethod), null, null, null);
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
	
	public static class NotApplicableResult 
	{
		@Override
		public String toString()
		{
			return "-not applicable-";
		}
	}
	
	@Override
	public Optional<DenyReason> getDenyReason()
	{
		return Optional.ofNullable(denyReason);
	}
}
