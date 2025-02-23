/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationPolicy;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision.ErrorDetail;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision.SecondFactorDetail;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision.UnknownRemoteUserDetail;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LastAuthenticationCookie;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.SessionCookie;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAccessCounter;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.session.SessionParticipants;

@Primary
@Component
class InteractiveAuthneticationProcessorImpl implements InteractiveAuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, InteractiveAuthneticationProcessorImpl.class);
	private final AuthenticationProcessor basicAuthnProcessor;
	private final EntityManagement entityMan;
	private final SessionManagement sessionMan;
	private final SessionParticipantTypesRegistry participantTypesRegistry;
	private final LoginToHttpSessionBinder sessionBinder;
	private final RememberMeProcessorImpl rememberMeProcessor;

	InteractiveAuthneticationProcessorImpl(AuthenticationProcessor basicAuthnProcessor,
			EntityManagement entityMan,
			SessionManagement sessionMan,
			SessionParticipantTypesRegistry participantTypesRegistry,
			LoginToHttpSessionBinder sessionBinder,
			RememberMeProcessorImpl rememberMeProcessor)
	{
		this.basicAuthnProcessor = basicAuthnProcessor;
		this.entityMan = entityMan;
		this.sessionMan = sessionMan;
		this.participantTypesRegistry = participantTypesRegistry;
		this.sessionBinder = sessionBinder;
		this.rememberMeProcessor = rememberMeProcessor;
	}

	@Override
	public PostAuthenticationStepDecision processFirstFactorResult(AuthenticationResult result,
			AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails,
			boolean setRememberMe,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			SessionReinitializer sessionReinitializer)
	{
		PartialAuthnState authnState;
		try
		{
			authnState = basicAuthnProcessor.processPrimaryAuthnResult(result,
					stepContext.selectedAuthnFlow, stepContext.authnOptionId);
			assertNotFailed(authnState.getPrimaryResult());
		} catch (AuthenticationException e)
		{
			return interpretAuthnException(e, httpRequest, machineDetails.getIp());
		}
		
		LoginSession loginSession = null;
		if (authnState.isSecondaryAuthenticationRequired())
		{
			Optional<LoginSession> loginSessionFromRememberMe = rememberMeProcessor
					.processRememberedSecondFactor(
							httpRequest,
							httpResponse,
							result.getSuccessResult().authenticatedEntity.getEntityId(),
							machineDetails.getIp(), stepContext.realm, getLoginCounter(httpRequest));
			if (!loginSessionFromRememberMe.isPresent())
			{
				log.debug("2nd factor authentication required for {} with {}", 
						authnState.getPrimaryResult().getSuccessResult().authenticatedEntity,
						authnState.getSecondaryAuthenticator().getAuthenticatorId());
				setLastIdpCookie(httpResponse, stepContext.authnOptionId, stepContext.endpointPath);
				return PostAuthenticationStepDecision.goToSecondFactor(new SecondFactorDetail(authnState));
			} else
			{
				loginSession = loginSessionFromRememberMe.get();
				log.debug("Second factor authn is remembered by entity "
						+ loginSession.getEntityId() + ", skipping it");
			}
		} else
		{
			loginSession = getLoginSessionForEntity(getAuthnContext(authnState.getPrimaryResult()),
					authnState.getPrimaryResult().getSuccessResult().authenticatedEntity,
					stepContext.realm, authnState.getFirstFactorOptionId(),
					null, Set.of(authnState.getPrimaryResult().getSuccessResult().authenticationMethod));
		}
		
		if (loginSession == null)
		{	
			throw new IllegalStateException("BUG: code tried to finalize authentication "
					+ "without login session");	
		}
		
		AuthenticatedEntity authnEntity = basicAuthnProcessor.finalizeAfterPrimaryAuthentication(
				authnState, loginSession.getRememberMeInfo().secondFactorSkipped);

		logged(authnEntity, loginSession, stepContext.realm, machineDetails, setRememberMe,
				AuthenticationProcessor.extractParticipants(result), sessionReinitializer, httpResponse);

		setLastIdpCookie(httpResponse, stepContext.authnOptionId, stepContext.endpointPath);
		return PostAuthenticationStepDecision.completed();
	}

	private void assertNotFailed(AuthenticationResult result)
	{
		if (result.getStatus() == Status.deny)
			throw new IllegalStateException("Exception should be thrown to signal authN failure");
	}

	@Override
	public PostAuthenticationStepDecision processSecondFactorResult(PartialAuthnState state,
			AuthenticationResult secondFactorResult,
			AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails,
			boolean setRememberMe,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			SessionReinitializer sessionReinitializer)
	{
		AuthenticatedEntity logInfo;
		try
		{
			logInfo = basicAuthnProcessor.finalizeAfterSecondaryAuthentication(state, secondFactorResult);
		} catch (AuthenticationException e)
		{
			return interpretAuthnException(e, httpRequest, machineDetails.getIp());
		}

		LoginSession loginSession = getLoginSessionForEntity(getAuthnContext(state.getPrimaryResult()), logInfo, stepContext.realm,
				state.getFirstFactorOptionId(), stepContext.authnOptionId,
				Set.of(state.getPrimaryResult()
						.getSuccessResult().authenticationMethod,
						secondFactorResult.getSuccessResult().authenticationMethod));

		List<SessionParticipant> sessionParticipants = AuthenticationProcessor.extractParticipants(
				state.getPrimaryResult(), secondFactorResult);
		logged(logInfo, loginSession, stepContext.realm, machineDetails, setRememberMe, sessionParticipants,
				sessionReinitializer, httpResponse);

		return PostAuthenticationStepDecision.completed();
	}

	@Override
	public PostAuthenticationStepDecision processRemoteRegistrationResult(AuthenticationResult result,
			AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails,
			HttpServletRequest httpRequest)
	{
		log.info("Processing results of remote authentication {}", result);
		if (log.isDebugEnabled())
			log.debug("Complete remote authn context:\n{}", result.toStringFull());
		try
		{
			basicAuthnProcessor.processPrimaryAuthnResult(result,
					stepContext.selectedAuthnFlow, stepContext.authnOptionId);
			return PostAuthenticationStepDecision.completed();
		} catch (AuthenticationException e)
		{
			return interpretAuthnException(e, httpRequest, machineDetails.getIp());
		}
	}
	
	@Override
	public void syntheticAuthenticate(RemoteAuthnMetadata authnContext, AuthenticatedEntity authenticatedEntity,
			List<SessionParticipant> participants,
			AuthenticationOptionKey authnOptionKey,
			AuthenticationRealm realm,
			LoginMachineDetails machineDetails,
			boolean setRememberMe,
			HttpServletResponse httpResponse,
			SessionReinitializer sessionReinitializer)
	{
		LoginSession loginSession = getLoginSessionForEntity(authnContext, authenticatedEntity, realm, authnOptionKey, null, Set.of(AuthenticationMethod.unkwown));
		logged(authenticatedEntity, loginSession, realm, machineDetails, setRememberMe, participants,
				sessionReinitializer, httpResponse);
	}
	
	@Override
	public PostAuthenticationStepDecision processFirstFactorSandboxAuthnResult(SandboxAuthenticationResult result,
			AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails,
			HttpServletRequest httpRequest,
			SandboxAuthnRouter sandboxRouter)
	{
		PartialAuthnState authnState;
		try
		{
			authnState = basicAuthnProcessor.processPrimaryAuthnResult(result, stepContext.selectedAuthnFlow, null);
			assertNotFailed(authnState.getPrimaryResult());
		} catch (UnknownRemoteUserException e)
		{
			sandboxRouter.fireEvent(new SandboxAuthnEvent(result.sandboxAuthnInfo, null,
					httpRequest.getSession() != null ? httpRequest.getSession().getId() : null));
			return PostAuthenticationStepDecision.completed();
		} catch (AuthenticationException e)
		{
			sandboxRouter.fireEvent(new SandboxAuthnEvent(
					RemoteSandboxAuthnContext.failedAuthn(result.sandboxAuthnInfo.getAuthnException().orElse(e),
							result.sandboxAuthnInfo.getLogs(),
							result.sandboxAuthnInfo.getRemotePrincipal()
									.map(RemotelyAuthenticatedPrincipal::getAuthnInput).orElse(null)),
					null, httpRequest.getSession().getId()));
			return interpretAuthnException(e, httpRequest, machineDetails.getIp());
		}

		if (authnState.isSecondaryAuthenticationRequired())
			return PostAuthenticationStepDecision.goToSecondFactor(new SecondFactorDetail(authnState));

		AuthenticatedEntity authnEntity = basicAuthnProcessor.finalizeAfterPrimaryAuthentication(authnState, false);
		sandboxRouter.fireEvent(
				new SandboxAuthnEvent(result.sandboxAuthnInfo, authnEntity, httpRequest.getSession().getId()));
		return PostAuthenticationStepDecision.completed();
	}
	
	@Override
	public PostAuthenticationStepDecision processSecondFactorSandboxAuthnResult(PartialAuthnState state,
			SandboxAuthenticationResult secondFactorResult,
			AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails,
			HttpServletRequest httpRequest,
			SandboxAuthnRouter sandboxRouter)
	{
		AuthenticatedEntity authnEntity;
		try
		{
			authnEntity = basicAuthnProcessor.finalizeAfterSecondaryAuthentication(state, secondFactorResult);
		} catch (AuthenticationException e)
		{
			sandboxRouter.fireEvent(new SandboxAuthnEvent(
					RemoteSandboxAuthnContext.failedAuthn(e, 
							secondFactorResult.sandboxAuthnInfo.getLogs(), 
							secondFactorResult.sandboxAuthnInfo.getRemotePrincipal()
								.map(RemotelyAuthenticatedPrincipal::getAuthnInput).orElse(null)), 
					null, 
					httpRequest.getSession().getId()));
			return interpretAuthnException(e, httpRequest, machineDetails.getIp());
		}
		sandboxRouter.fireEvent(new SandboxAuthnEvent(secondFactorResult.sandboxAuthnInfo, 
				authnEntity, httpRequest.getSession().getId()));
		return PostAuthenticationStepDecision.completed();
	}
	
	private PostAuthenticationStepDecision interpretAuthnException(AuthenticationException e,
			HttpServletRequest httpRequest,
			String ip)
	{
		UnsuccessfulAccessCounter counter = getLoginCounter(httpRequest);
		if (e instanceof UnknownRemoteUserException)
		{
			log.info("Unknown remote user for {}", e.getResult().asRemote().getUnknownRemotePrincipalResult());
			return PostAuthenticationStepDecision.unknownRemoteUser(
					new UnknownRemoteUserDetail(e.getResult().asRemote().getUnknownRemotePrincipalResult()));
		} else
		{
			log.info("Authentication failure: {} {}", e.getMessage(), e.getResult());
			counter.unsuccessfulAttempt(ip);
			return PostAuthenticationStepDecision.error(new ErrorDetail(new ResolvableError(e.getMessage())));
		}
	}
	
	private LoginSession getLoginSessionForEntity(RemoteAuthnMetadata authnContext, AuthenticatedEntity authenticatedEntity,
			AuthenticationRealm realm,
			AuthenticationOptionKey firstFactorAuhtnOptionId,
			AuthenticationOptionKey secondFactorAuhtnOptionId, Set<AuthenticationMethod> authenticationMethods)
	{

		long entityId = authenticatedEntity.getEntityId();
		String label = getLabel(entityId);
		return sessionMan.getCreateSession(entityId, realm, label,
				authenticatedEntity.getOutdatedCredentialId(), 
				new RememberMeInfo(false, false), firstFactorAuhtnOptionId,
				secondFactorAuhtnOptionId, authnContext, authenticationMethods);
	}
	
	private RemoteAuthnMetadata getAuthnContext(AuthenticationResult authenticationResult)
	{
		if (authenticationResult.isRemote())
		{
			return authenticationResult.asRemote().getSuccessResult().getRemotelyAuthenticatedPrincipal().getAuthnInput().getRemoteAuthnMetadata();
		}
	
		return null;	
	}
	
	private void logged(AuthenticatedEntity authenticatedEntity,
			LoginSession ls,
			AuthenticationRealm realm,
			LoginMachineDetails machineDetails,
			boolean rememberMe,
			List<SessionParticipant> participants,
			SessionReinitializer sessionReinitializer,
			HttpServletResponse httpResponse)
	{
		sessionMan.updateSessionAttributes(ls.getId(), 
				new SessionParticipants.AddParticipantToSessionTask(
						participantTypesRegistry,
						participants.toArray(new SessionParticipant[participants.size()])));

		//prevent session fixation
		HttpSession httpSession = sessionReinitializer.reinitialize();
		bindReinitializedHttpSession(httpSession, ls, realm);

		if (rememberMe)
		{
			rememberMeProcessor.addRememberMeCookieAndUnityToken(httpResponse, realm, machineDetails, ls.getEntityId(),
					ls.getStarted(), ls.getLogin1stFactorOptionId(), ls.getLogin2ndFactorOptionId());
		}

		addSessionCookie(realm.getName(), ls.getId(), httpResponse);
		
		ls.addAuthenticatedIdentities(authenticatedEntity.getAuthenticatedWith());
		ls.setRemoteIdP(authenticatedEntity.getRemoteIdP());
		if (ls.isUsedOutdatedCredential())
			log.info("User {} logged with outdated credential", ls.getEntityId());
		AuthenticationPolicy.setPolicy(httpSession, AuthenticationPolicy.DEFAULT);

		log.info("Logged with session: {}, first factor authn option: {}, second factor authn option: {}"
				+ ", first factor skipped: {}, second factor skipped: {}",
				ls.toString(), ls.getLogin1stFactorOptionId(), ls.getLogin2ndFactorOptionId(),
				ls.getRememberMeInfo().firstFactorSkipped, ls.getRememberMeInfo().secondFactorSkipped);
	}

	private void bindReinitializedHttpSession(HttpSession httpSession, LoginSession ls, AuthenticationRealm realm)
	{
		LoginSession previouslyBoundLoginSession = (LoginSession)httpSession.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		boolean httpSessionWasAlreadyBoundToThisLoginSession = previouslyBoundLoginSession != null
				&& previouslyBoundLoginSession.getId().equals(ls.getId());
		//this may be false during synthetic authentication into another realm
		// or simply when we don't have realm set yet in invocation context. Ideally we should always have it,
		// would require to split InvocationContextSetupFilter into two
		boolean authenticationRealmMatchesCurrentRealm = realm.getName().equals(InvocationContext.safeGetRealm());
		if (httpSessionWasAlreadyBoundToThisLoginSession || authenticationRealmMatchesCurrentRealm)
			sessionBinder.bindHttpSession(httpSession, ls);
	}

	private static UnsuccessfulAccessCounter getLoginCounter(HttpServletRequest httpRequest)
	{
		UnsuccessfulAccessCounter servletSet = (UnsuccessfulAccessCounter) 
				httpRequest.getServletContext().getAttribute(UnsuccessfulAccessCounter.class.getName());
		if (servletSet == null)
			throw new IllegalStateException("No authn failures counter in servlet context");
		return servletSet;
	}
	
	private String getLabel(long entityId)
	{
		try
		{
			return entityMan.getEntityLabel(new EntityParam(entityId));
		} catch (AuthorizationException e)
		{
			log.debug("Not setting entity's label as the client is not authorized to read the attribute",
					e);
		} catch (EngineException e)
		{
			log.error("Can not get the attribute designated with EntityName", e);
		}
		return null;
	}
	
	private void addSessionCookie(String realmName, String sessionId, HttpServletResponse servletResponse)
	{
		servletResponse.addCookie(new SessionCookie(realmName, sessionId).toHttpCookie());
	}
	
	private void setLastIdpCookie(HttpServletResponse response, AuthenticationOptionKey idpKey, String endpointPath)
	{
		if (endpointPath == null)
			return;
		Optional<Cookie> lastIdpCookie = LastAuthenticationCookie.createLastIdpCookie(endpointPath, idpKey);
		lastIdpCookie.ifPresent(response::addCookie);
	}
}
