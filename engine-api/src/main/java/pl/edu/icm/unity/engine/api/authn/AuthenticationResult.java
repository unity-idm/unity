/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.Optional;

import pl.edu.icm.unity.MessageSource;

/**
 * Base contract of authentication result - have remote and local authn variants.
 */
public interface AuthenticationResult
{
	public enum Status {
		/**
		 * There was no input for authenticator
		 */
		notApplicable, 
		
		/**
		 * There was authentication try with the authenticator but it failed
		 */
		deny, 
		
		/**
		 * Can happen only in the case of remote authenticators, when the 
		 * authentication was successful, but the remote principal is not 
		 * registered locally. 
		 */
		unknownRemotePrincipal, 

		/**
		 * Everything OK
		 */
		success
	}
	
	public enum DenyReason
	{
		notDefinedCredential
	}
	
	Status getStatus();
	
	Optional<DenyReason> getDenyReason();
	
	boolean isRemote();
	
	String toStringFull();
	
	SuccessResult getSuccessResult();
	
	ErrorResult getErrorResult();
	
	default RemoteAuthenticationResult asRemote()
	{
		if (!isRemote())
			throw new IllegalStateException("This is not a remote result");
		return (RemoteAuthenticationResult)this;
	}
	
	default LocalAuthenticationResult asLocal()
	{
		if (isRemote())
			throw new IllegalStateException("This is not a local result");
		return (LocalAuthenticationResult)this;
	}
	
	class ErrorResult 
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
	
	class SuccessResult 
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
	
	
	class ResolvableError
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
