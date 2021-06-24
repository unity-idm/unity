/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;

/**
 * Handles authentication results for interactive authentications (typically over some web binding).
 * Supports features like remember me etc.
 */
public interface InteractiveAuthenticationProcessor
{
	static final String UNITY_SESSION_COOKIE_PFX = "USESSIONID_";
	
	PostFirstFactorAuthnDecision processFirstFactorResult(AuthenticationResult result, AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails, boolean setRememberMe,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse);
	
	
	
	public class PostFirstFactorAuthnDecision
	{
		public enum Decision {COMPLETED, GO_TO_2ND_FACTOR, ERROR, UNKNOWN_REMOTE_USER}
		
		private final Decision decision;
		
		private final UnknownRemoteUserDetail unknownRemoteUserDetail;
		private final ErrorDetail errorDetail;
		private final SecondFactorDetail secondFactorDetail;
	
		private PostFirstFactorAuthnDecision(Decision decision, UnknownRemoteUserDetail unknownRemoteUserDetail,
				ErrorDetail errorDetail, SecondFactorDetail secondFactorDetail)
		{
			this.decision = decision;
			this.unknownRemoteUserDetail = unknownRemoteUserDetail;
			this.errorDetail = errorDetail;
			this.secondFactorDetail = secondFactorDetail;
		}

		public static PostFirstFactorAuthnDecision unknownRemoteUser(UnknownRemoteUserDetail unknownRemoteUserDetail)
		{
			return new PostFirstFactorAuthnDecision(Decision.UNKNOWN_REMOTE_USER, unknownRemoteUserDetail, null, null);
		}

		public static PostFirstFactorAuthnDecision error(ErrorDetail errorDetail)
		{
			return new PostFirstFactorAuthnDecision(Decision.ERROR, null, errorDetail, null);
		}

		public static PostFirstFactorAuthnDecision goToSecondFactor(SecondFactorDetail secondFactorDetail)
		{
			return new PostFirstFactorAuthnDecision(Decision.GO_TO_2ND_FACTOR, null, null, secondFactorDetail);
		}

		public static PostFirstFactorAuthnDecision completed()
		{
			return new PostFirstFactorAuthnDecision(Decision.COMPLETED, null, null, null);
		}

		public Decision getDecision()
		{
			return decision;
		}

		public UnknownRemoteUserDetail getUnknownRemoteUserDetail()
		{
			if (decision != Decision.UNKNOWN_REMOTE_USER)
				throw new IllegalStateException("Unknown remote user detail not available in state: " + decision);
			return unknownRemoteUserDetail;
		}

		public ErrorDetail getErrorDetail()
		{
			if (decision != Decision.ERROR)
				throw new IllegalStateException("Error detail not available in state: " + decision);
			return errorDetail;
		}

		public SecondFactorDetail getSecondFactorDetail()
		{
			if (decision != Decision.GO_TO_2ND_FACTOR)
				throw new IllegalStateException("2nd factor detail not available in state: " + decision);
			return secondFactorDetail;
		}



		public static class UnknownRemoteUserDetail
		{
			public final UnknownRemotePrincipalResult unknownRemotePrincipal;

			public UnknownRemoteUserDetail(UnknownRemotePrincipalResult unknownRemotePrincipal)
			{
				this.unknownRemotePrincipal = unknownRemotePrincipal;
			}
		}

		public static class ErrorDetail
		{
			public final ResolvableError error;

			public ErrorDetail(ResolvableError error)
			{
				this.error = error;
			}
		}

		public static class SecondFactorDetail
		{
		}
	}
	
	
}
