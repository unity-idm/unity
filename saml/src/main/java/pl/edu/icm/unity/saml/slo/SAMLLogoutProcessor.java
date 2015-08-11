/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.slo.SAMLInternalLogoutContext.AsyncLogoutFinishCallback;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.registries.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration.LogoutMode;
import pl.edu.icm.unity.types.basic.EntityParam;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestType;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.LogoutResponse;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.LogoutRequestValidator;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.security.dsig.DSigException;

/**
 * Implements handling of logout requests received via SAML with any binding. Handling of async and sync bindings 
 * is naturally implemented differently. Its main co-worker is {@link InternalLogoutProcessor} which handles 
 * logout of additional session participants. 
 * 
 * 
 * @author K. Benedyczak
 */
public class SAMLLogoutProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLLogoutProcessor.class);
	
	private SessionManagement sessionManagement;
	private LogoutMode logoutMode;
	private SessionParticipantTypesRegistry registry;
	private IdentityResolver idResolver;
	private LogoutContextsStore contextsStore;
	private ReplayAttackChecker replayChecker;
	private SLOAsyncResponseHandler responseHandler;
	private InternalLogoutProcessor internalProcessor;
	private IdentityTypeMapper identityTypeMapper;
	private String consumerEndpointUri;
	private long requestValidity;
	private String localSamlId;
	private X509Credential localSamlCredential;
	private SamlTrustProvider trustProvider;
	private String realm;

	/**
	 * Ouch ;-) Probably we should encapsulate non bean params into a config class. But we have a factory to help.
	 * @param sessionManagement
	 * @param idResolver
	 * @param contextsStore
	 * @param replayChecker
	 * @param responseHandler
	 * @param internalProcessor
	 * @param identityTypeMapper
	 * @param consumerEndpointUri
	 * @param requestValidity
	 * @param localSamlId
	 * @param localSamlCredential
	 * @param trustChecker
	 * @param realm
	 */
	public SAMLLogoutProcessor(SessionManagement sessionManagement, SessionParticipantTypesRegistry registry,
			IdentityResolver idResolver, LogoutContextsStore contextsStore,
			ReplayAttackChecker replayChecker, SLOAsyncResponseHandler responseHandler,
			InternalLogoutProcessor internalProcessor,
			IdentityTypeMapper identityTypeMapper, String consumerEndpointUri,
			long requestValidity, String localSamlId,
			X509Credential localSamlCredential, SamlTrustProvider trustProvider,
			String realm, UnityServerConfiguration mainConfig)
	{
		this.sessionManagement = sessionManagement;
		this.registry = registry;
		this.idResolver = idResolver;
		this.contextsStore = contextsStore;
		this.replayChecker = replayChecker;
		this.responseHandler = responseHandler;
		this.internalProcessor = internalProcessor;
		this.identityTypeMapper = identityTypeMapper;
		this.consumerEndpointUri = consumerEndpointUri;
		this.requestValidity = requestValidity;
		this.localSamlId = localSamlId;
		this.localSamlCredential = localSamlCredential;
		this.trustProvider = trustProvider;
		this.realm = realm;
		this.logoutMode = mainConfig.getEnumValue(UnityServerConfiguration.LOGOUT_MODE, LogoutMode.class);
	}

	/**
	 * Handles logout request initiated by a synchronous (SOAP) binding. All logouts of session participants 
	 * can happen only using the synchronous binding. After performing the logout a response is returned. 
	 * @param request
	 * @return
	 * @throws SAMLServerException 
	 */
	public LogoutResponseDocument handleSynchronousLogoutFromSAML(LogoutRequestDocument request) 
	{
		try
		{
			SAMLExternalLogoutContext externalCtx = initFromSAML(request, null, Binding.SOAP, false);
			SAMLInternalLogoutContext internalCtx = new SAMLInternalLogoutContext(externalCtx.getSession(), 
					request.getLogoutRequest().getIssuer().getStringValue(), null, registry);
			if (logoutMode != LogoutMode.internalOnly)
			{
				internalProcessor.logoutSynchronousParticipants(internalCtx);
			}
			boolean partial = !internalCtx.allCorrectlyLoggedOut();
			sessionManagement.removeSession(internalCtx.getSession().getId(), false);
			LogoutResponseDocument finalResponse = prepareFinalLogoutResponse(externalCtx, 
					externalCtx.getInitiator().getLogoutEndpoints().get(Binding.SOAP), 
					partial);
			return finalResponse;
		} catch (SAMLServerException e)
		{
			log.debug("SOAP Logout request processing finished with error, "
					+ "converting it to SAML error response", e);
			LogoutResponse responseDoc = new LogoutResponse(getIssuer(localSamlId), 
					request.getLogoutRequest().getID(), e);
			return responseDoc.getXMLBeanDoc();
		}
	}
	
	/**
	 * Handles logout request initiated by an asynchronous binding (HTTP POST or Redirect). The method either can 
	 * return the response to the requester (by return redirect returned to the client's agent) or request 
	 * by redirection to one of additional session participants.
	 *  
	 * @param request
	 * @param response
	 * @throws EopException 
	 * @throws IOException 
	 */
	public void handleAsyncLogoutFromSAML(LogoutRequestDocument request, String relayState, 
			HttpServletResponse response, Binding binding) throws IOException, EopException
	{
		SAMLExternalLogoutContext externalCtx;
		try
		{
			externalCtx = initFromSAML(request, relayState, binding, true);
		} catch (SAMLServerException e)
		{
			handleEarlyError(e, request, relayState, response, binding);
			return;
		}
		log.debug("Handling SAML logout request from " + externalCtx.getRequest().getIssuer().getStringValue());
		
		AsyncLogoutFinishCallback finishCallback = new AsyncLogoutFinishCallback()
		{
			@Override
			public void finished(HttpServletResponse response,
					SAMLInternalLogoutContext finalInternalContext)
			{
				try
				{
					internalLogoutFinished(response, finalInternalContext);
				} catch (EopException e)
				{
					//ok
				}
			}
		};
		
		SAMLInternalLogoutContext internalCtx = new SAMLInternalLogoutContext(externalCtx.getSession(), 
				request.getLogoutRequest().getIssuer().getStringValue(), finishCallback, registry);
		internalCtx.setRelayState(externalCtx.getInternalRelayState());
		switch (logoutMode)
		{
		case internalAndAsyncPeers:
			contextsStore.addInternalContext(externalCtx.getInternalRelayState(), internalCtx);
			internalProcessor.continueAsyncLogout(internalCtx, response);
			break;
		case internalAndSyncPeers:
			internalProcessor.logoutSynchronousParticipants(internalCtx);
			internalLogoutFinished(response, internalCtx);
			break;
		case internalOnly:
			internalLogoutFinished(response, internalCtx);
			break;
		}
	}

	/**
	 * Careful handling of early errors when handling the SAML request. This code does not assume request is valid 
	 * nor trusted. If it is possible the error response is sent back. If not an error page is presented.
	 * @param e
	 * @param request
	 * @param relayState
	 * @param response
	 * @param binding
	 * @throws EopException 
	 * @throws IOException 
	 */
	private void handleEarlyError(SAMLServerException error, LogoutRequestDocument request, String relayState, 
			HttpServletResponse response, Binding binding) throws IOException, EopException
	{
		NameIDType issuer = request.getLogoutRequest().getIssuer();
		if (issuer == null || issuer.getStringValue() == null)
			responseHandler.showError(new SAMLProcessingException(
					"A logout process can not be started", error), response);
		
		Collection<SAMLEndpointDefinition> sloEndpoints = trustProvider.getSLOEndpoints(issuer);
		if (sloEndpoints == null)
			responseHandler.showError(new SAMLProcessingException(
					"A logout process can not be started", error), response);
		SAMLEndpointDefinition sloEndpoint = null;
		for (SAMLEndpointDefinition samlEndpoint: sloEndpoints)
			if (samlEndpoint.getBinding() == binding)
			{
				sloEndpoint = samlEndpoint;
				break;
			}
		if (sloEndpoint == null)
			responseHandler.showError(new SAMLProcessingException(
					"A logout process can not be started", error), response);
		responseHandler.sendErrorResponse(binding, error, sloEndpoint.getReturnUrl(), localSamlId,
				relayState, request.getLogoutRequest().getID(), response);
	}
	
	/**
	 * Initializes the logout process when started by means of SAML protocol: 
	 * request is validated, login session resolved, authorization is checked.
	 *  Then the logout context is created, stored and persisted.  
	 * @param request
	 * @return
	 * @throws SAMLServerException
	 */
	private SAMLExternalLogoutContext initFromSAML(LogoutRequestDocument request, String requesterRelayState, 
			Binding binding, boolean persistContext) throws SAMLServerException
	{
		LoginSession session = resolveRequest(request);
		SAMLExternalLogoutContext ctx = new SAMLExternalLogoutContext(localSamlId, request,  
				requesterRelayState, binding, session, registry);
		if (ctx.getInitiator() == null)
			throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED,
					"The request issuer is not among session participants");
		if (!ctx.getInitiator().getLogoutEndpoints().containsKey(binding))
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED,
					"The request issuer has no logout endpoint defined "
					+ "with a binding used to submit the request: " + binding);
		if (persistContext)
			contextsStore.addSAMLExternalContext(ctx);
		return ctx;
	}
	
	private void internalLogoutFinished(HttpServletResponse response,
			SAMLInternalLogoutContext finalInternalContext) throws EopException
	{
		String relayState = finalInternalContext.getRelayState();
		boolean partial = !finalInternalContext.allCorrectlyLoggedOut();
		SAMLExternalLogoutContext externalCtx = contextsStore.getSAMLExternalContext(
				relayState);
		try
		{
			if (externalCtx == null)
			{
				log.error("Can not find SAML external logout context " + relayState);
				responseHandler.showError(new SAMLProcessingException(
						"Can not find SAML external logout context"), response);
				return;
			}
			finishAsyncLogoutFromSAML(externalCtx, partial,	response, relayState);
		} catch (IOException e)
		{
			log.error("Finalization of logout failed", e);
			try
			{
				responseHandler.showError(new SAMLProcessingException(
						"Internal error handling logout request"), response);
			} catch (IOException ee)
			{
				log.error("Showing error failed", ee);
			}
		}
	}
	
	/**
	 * Prepares the final response and sends it back via async binding
	 * @throws EopException 
	 * @throws IOException 
	 */
	private void finishAsyncLogoutFromSAML(SAMLExternalLogoutContext ctx, boolean partial, 
			HttpServletResponse response, String externalContextKey) throws IOException, EopException
	{
		sessionManagement.removeSession(ctx.getSession().getId(), false);
		Binding binding = ctx.getRequestBinding();
		SAMLEndpointDefinition endpoint = ctx.getInitiator().getLogoutEndpoints().get(binding);
		LogoutResponseDocument finalResponse;
		try
		{
			finalResponse = prepareFinalLogoutResponse(ctx, endpoint, partial);
		} catch (SAMLResponderException e)
		{
			responseHandler.sendErrorResponse(binding, e, endpoint.getReturnUrl(), ctx, response);
			return;
		}

		contextsStore.removeSAMLExternalContext(externalContextKey);
		responseHandler.sendResponse(binding, finalResponse, endpoint.getReturnUrl(), ctx, response);
	}

	/**
	 * Prepares the final logout response, taking into account 
	 * the overall logout state from the context.
	 * @param ctx
	 * @return
	 * @throws SAMLResponderException 
	 */
	private LogoutResponseDocument prepareFinalLogoutResponse(SAMLExternalLogoutContext ctx, 
			SAMLEndpointDefinition endpoint, boolean partial) 
			throws SAMLResponderException
	{
		LogoutResponse response = new LogoutResponse(getIssuer(ctx.getLocalSessionAuthorityId()), 
				ctx.getRequest().getID());
		response.getXMLBean().setDestination(endpoint.getReturnUrl());
		if (partial)
			response.setPartialLogout();
		try
		{
			response.sign(localSamlCredential.getKey(), localSamlCredential.getCertificateChain());
		} catch (DSigException e)
		{
			log.warn("Unable to sign SLO response", e);
			throw new SAMLResponderException("Internal server error signing response.");
		}
		return response.getXMLBeanDoc();
	}
	
	/**
	 * Validates the logout request and searches for an appropriate session which is returned.
	 * @param request
	 * @return
	 * @throws SAMLServerException 
	 */
	private LoginSession resolveRequest(LogoutRequestDocument request) throws SAMLServerException
	{
		LogoutRequestValidator validator = new LogoutRequestValidator(consumerEndpointUri, 
				trustProvider.getTrustChecker(), requestValidity, replayChecker);
		validator.validate(request);
		
		LogoutRequestType logoutRequest = request.getLogoutRequest();
		NameIDType loggedOut = logoutRequest.getNameID();
		String samlType = loggedOut.getFormat();
		if (samlType == null)
			samlType = SAMLConstants.NFORMAT_UNSPEC;
		String unityType = identityTypeMapper.mapIdentity(loggedOut.getFormat());
		String identity = loggedOut.getStringValue();
		
		long localEntity;
		try
		{
			localEntity = idResolver.resolveIdentity(identity, new String[] {unityType}, 
					request.getLogoutRequest().getIssuer().getStringValue(), realm);
		} catch (EngineException e)
		{
			throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_UNKNOWN_PRINCIPIAL,
					"The principal is not known");
		}
		
		try
		{
			return sessionManagement.getOwnedSession(new EntityParam(localEntity), realm);
		} catch (EngineException e)
		{
			throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_NO_AUTHN_CONTEXT,
					"The login session was not found");
		}
	}
	
	private NameIDType getIssuer(String localSamlId)
	{
		return new NameID(localSamlId, SAMLConstants.NFORMAT_ENTITY).getXBean();
	}
	
	/**
	 * Implementation provides access to saml trust checker. It is used as SAML trust settings may easily 
	 * change at runtime.
	 * @author K. Benedyczak
	 */
	public interface SamlTrustProvider
	{
		SamlTrustChecker getTrustChecker();
		Collection<SAMLEndpointDefinition> getSLOEndpoints(NameIDType samlEntity);
	}
}
