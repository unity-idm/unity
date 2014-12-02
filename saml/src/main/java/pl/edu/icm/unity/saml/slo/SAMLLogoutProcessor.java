/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

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
			String realm)
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
			internalProcessor.logoutSynchronousParticipants(internalCtx);
			boolean allLoggedOut = internalCtx.getFailed().isEmpty();
			sessionManagement.removeSession(internalCtx.getSession().getId(), false);
			LogoutResponseDocument finalResponse = prepareFinalLogoutResponse(externalCtx, !allLoggedOut);
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
			responseHandler.showError(new SAMLProcessingException(
					"A logout process can not be started", e), response);
			return;
		}
		
		AsyncLogoutFinishCallback finishCallback = new AsyncLogoutFinishCallback()
		{
			@Override
			public void finished(HttpServletResponse response,
					SAMLInternalLogoutContext finalInternalContext)
			{
				internalLogoutFinished(response, finalInternalContext);
			}
		};
		
		SAMLInternalLogoutContext internalCtx = new SAMLInternalLogoutContext(externalCtx.getSession(), 
				request.getLogoutRequest().getIssuer().getStringValue(), finishCallback, registry);
		contextsStore.addInternalContext(externalCtx.getRequestersRelayState(), internalCtx);
		
		internalProcessor.continueAsyncLogout(internalCtx, response);
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
				requesterRelayState, binding, session);
		if (ctx.getInitiator() == null)
			throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED,
					"The request issuer is not among session participants");
		if (!ctx.getInitiator().getLogoutEndpoints().containsKey(binding))
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED,
					"The request issuer has no logout endpoint defined "
					+ "with a binding used to submit the request: " + binding);
		if (persistContext)
			contextsStore.addExternalContext(ctx);
		return ctx;
	}
	
	private void internalLogoutFinished(HttpServletResponse response,
			SAMLInternalLogoutContext finalInternalContext)
	{
		String relayState = finalInternalContext.getRelayState();
		boolean partial = !finalInternalContext.getFailed().isEmpty();
		SAMLExternalLogoutContext externalCtx = contextsStore.getExternalContext(
				relayState);
		try
		{
			finishAsyncLogoutFromSAML(externalCtx, partial,	response, relayState);
		} catch (IOException e)
		{
			log.error("Finalization of logout failed", e);
		} catch (EopException e)
		{
			//ok
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
			finalResponse = prepareFinalLogoutResponse(ctx, partial);
		} catch (SAMLResponderException e)
		{
			responseHandler.sendErrorResponse(binding, e, endpoint.getReturnUrl(), ctx, response);
			return;
		}

		contextsStore.removeExternalContext(externalContextKey);
		responseHandler.sendResponse(binding, finalResponse, endpoint.getReturnUrl(), ctx, response);
	}

	/**
	 * Prepares the final logout response, taking into account 
	 * the overall logout state from the context.
	 * @param ctx
	 * @return
	 * @throws SAMLResponderException 
	 */
	private LogoutResponseDocument prepareFinalLogoutResponse(SAMLExternalLogoutContext ctx, boolean partial) 
			throws SAMLResponderException
	{
		LogoutResponse response = new LogoutResponse(getIssuer(ctx.getLocalSessionAuthorityId()), 
				ctx.getRequest().getID());
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
			localEntity = idResolver.resolveIdentity(identity, new String[] {unityType});
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
	}
}
