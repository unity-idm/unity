/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult.NotApplicableResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

/**
 * This class object is returned by authenticator with information about authentication result. 
 * This cover authentication result of a single authenticator, not the combined result of authentication
 * with all authenticators in the set.
 */
public class RemoteAuthenticationResult implements AuthenticationResult
{
	private final Status status;
	private final RemoteSuccessResult successResult;
	private final NotApplicableResult notApplicableResult;
	private final UnknownRemotePrincipalResult unknownRemotePrincipalResult;
	private final RemoteErrorResult errorResult;
	
	private RemoteAuthenticationResult(Status status, 
			RemoteSuccessResult successResult,
			NotApplicableResult notApplicableResult, 
			UnknownRemotePrincipalResult unknownRemotePrincipalResult,
			RemoteErrorResult errorResult)
	{
		this.status = status;
		this.successResult = successResult;
		this.notApplicableResult = notApplicableResult;
		this.unknownRemotePrincipalResult = unknownRemotePrincipalResult;
		this.errorResult = errorResult;
	}
	
	protected RemoteAuthenticationResult(RemoteAuthenticationResult toClone)
	{
		this(toClone.status, toClone.successResult, toClone.notApplicableResult, 
				toClone.unknownRemotePrincipalResult, toClone.errorResult);
	}

	public static RemoteAuthenticationResult notApplicable()
	{
		return new RemoteAuthenticationResult(Status.notApplicable, null, new NotApplicableResult(), null, null);
	}
	
	public static RemoteAuthenticationResult failed(RemotelyAuthenticatedPrincipal remotePrincipal, Exception cause, 
			ResolvableError error)
	{
		return new RemoteAuthenticationResult(Status.deny, null, null, null, 
				new RemoteErrorResult(error, cause, remotePrincipal));
	}
	
	public static RemoteAuthenticationResult failed(RemotelyAuthenticatedPrincipal remotePrincipal, ResolvableError error)
	{
		return failed(remotePrincipal, null, error);
	}
	
	public static RemoteAuthenticationResult failed()
	{
		return failed(null);
	}
	
	public static RemoteAuthenticationResult failed(Exception cause)
	{
		return failed(null, cause, ResolvableError.EMPTY);
	}

	public static RemoteAuthenticationResult successful(RemotelyAuthenticatedPrincipal remotePrincipal,
			AuthenticatedEntity authenticatedEntity, AuthenticationMethod authenticationMethod)
	{
		checkNotNull(authenticatedEntity);
		checkNotNull(remotePrincipal);
		return new RemoteAuthenticationResult(Status.success, new RemoteSuccessResult(authenticatedEntity, remotePrincipal, authenticationMethod), 
				null, null, null);
	}

	public static RemoteAuthenticationResult successfulPartial(RemotelyAuthenticatedPrincipal remotePrincipal,
			AuthenticatedEntity authenticatedEntity, AuthenticationMethod authenticationMethod)
	{
		checkNotNull(remotePrincipal);
		return new RemoteAuthenticationResult(Status.success, new RemoteSuccessResult(authenticatedEntity, remotePrincipal, authenticationMethod), 
				null, null, null);
	}
	
	public static RemoteAuthenticationResult unknownRemotePrincipal(RemotelyAuthenticatedPrincipal remotePrincipal,
			String formForUnknownPrincipal, boolean enableAssociation)
	{
		checkNotNull(remotePrincipal);
		return new RemoteAuthenticationResult(Status.unknownRemotePrincipal, null, 
				null, new UnknownRemotePrincipalResult(remotePrincipal, formForUnknownPrincipal, enableAssociation), null);
	}
	
	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public boolean isRemote()
	{
		return true;
	}

	@Override
	public RemoteSuccessResult getSuccessResult()
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

	public UnknownRemotePrincipalResult getUnknownRemotePrincipalResult()
	{
		if (status != Status.unknownRemotePrincipal)
			throw new IllegalStateException("Can be aonly called on unknownRemotePrincipal result, but we are " + status);
		return unknownRemotePrincipalResult;
	}

	@Override
	public RemoteErrorResult getErrorResult()
	{
		if (status != Status.deny)
			throw new IllegalStateException("Can be aonly called on deny result, but we are " + status);
		return errorResult;
	}

	public RemotelyAuthenticatedPrincipal getRemotelyAuthenticatedPrincipal()
	{
		Object detail = getCurrentDetail();
		if (detail instanceof RemotePrincipalProvider)
			return ((RemotePrincipalProvider)detail).getRemotelyAuthenticatedPrincipal();
		throw new IllegalStateException("Can't access RemotelyAuthenticatedPrincipal on result of state " + status);
	}
	
	@Override
	public String toStringFull()
	{
		return "RemoteAuthenticationResult: \nstatus=" + status + "\ndetails=" + getCurrentDetail();
	}

	private Object getCurrentDetail()
	{
		return successResult != null ? successResult : 
			notApplicableResult != null ? notApplicableResult : 
				errorResult != null ? errorResult : unknownRemotePrincipalResult;
	}
	
	@Override
	public String toString()
	{
		return status.toString();
	}
	
	public static class RemoteErrorResult extends ErrorResult implements RemotePrincipalProvider
	{
		public final RemotelyAuthenticatedPrincipal remotePrincipal;
		
		RemoteErrorResult(ResolvableError error, Exception cause, RemotelyAuthenticatedPrincipal remotePrincipal)
		{
			super(error, cause);
			this.remotePrincipal = remotePrincipal;
		}

		@Override
		public String toString()
		{
			return String.format("[remoteAuthnContext=%s, error=%s]",
					remotePrincipal, error);
		}

		@Override
		public RemotelyAuthenticatedPrincipal getRemotelyAuthenticatedPrincipal()
		{
			return remotePrincipal;
		}
	}
	
	public static class UnknownRemotePrincipalResult implements RemotePrincipalProvider
	{
		public final RemotelyAuthenticatedPrincipal remotePrincipal;
		public final String formForUnknownPrincipal;
		public final boolean enableAssociation;

		public UnknownRemotePrincipalResult(RemotelyAuthenticatedPrincipal remotePrincipal,
				String formForUnknownPrincipal, boolean enableAssociation)
		{
			this.remotePrincipal = remotePrincipal;
			this.formForUnknownPrincipal = formForUnknownPrincipal;
			this.enableAssociation = enableAssociation;
		}

		@Override
		public String toString()
		{
			return String.format(
					"[remoteAuthnContext=%s, formForUnknownPrincipal=%s, enableAssociation=%s]",
					remotePrincipal, formForUnknownPrincipal, enableAssociation);
		}

		@Override
		public RemotelyAuthenticatedPrincipal getRemotelyAuthenticatedPrincipal()
		{
			return remotePrincipal;
		}
	}

	public static class RemoteSuccessResult extends SuccessResult implements RemotePrincipalProvider
	{
		public final RemotelyAuthenticatedPrincipal remotePrincipal;

		public RemoteSuccessResult(AuthenticatedEntity authenticatedEntity,
				RemotelyAuthenticatedPrincipal remotePrincipal, AuthenticationMethod authenticationMethod)
		{
			super(authenticatedEntity, authenticationMethod);
			this.remotePrincipal = remotePrincipal;
		}

		@Override
		public String toString()
		{
			return String.format("[remoteAuthnContext=%s]", remotePrincipal);
		}

		@Override
		public RemotelyAuthenticatedPrincipal getRemotelyAuthenticatedPrincipal()
		{
			return remotePrincipal;
		}
	}

	private interface RemotePrincipalProvider
	{
		RemotelyAuthenticatedPrincipal getRemotelyAuthenticatedPrincipal();
	}

	@Override
	public Optional<DenyReason> getDenyReason()
	{
		return Optional.empty();
	}
}
