/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.EntityParam;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestType;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;
import xmlbeans.org.oasis.saml2.protocol.StatusType;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.proto.LogoutRequest;
import eu.unicore.samly2.proto.LogoutResponse;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.LogoutRequestValidator;
import eu.unicore.samly2.validators.LogoutResponseValidator;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.wsutil.samlclient.SAMLLogoutClient;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Engine class. Implements actual logout: initiates new logout processes, manages asynchronous logouts 
 * and produces final response.
 * @author K. Benedyczak
 */
public class LogoutProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, LogoutProcessor.class);
	
	//coworkers
	private SessionManagement sessionManagement;
	private IdentityResolver idResolver;
	
	private IdentityTypeMapper identityTypeMapper;
	private LogoutContextsStore contextsStore;
	private SLOAsyncResponseHandler responseHandler;
	private ReplayAttackChecker replayChecker;
	
	//configuration
	private String consumerEndpointUri;
	private SamlTrustChecker trustChecker;
	private long requestValidity;
	private String realm;
	private String localSamlId;
	private X509Credential localSamlCredential;
	private IClientConfiguration soapClientConfig;
	
	/**
	 * Handles logout request initiated by a synchronous (SOAP) binding. All logouts of session participants 
	 * can happen only using the synchronous binding. After performing the logout a response is returned. 
	 * @param request
	 * @return
	 * @throws SAMLServerException 
	 */
	public LogoutResponseDocument handleSynchronousLogout(LogoutRequestDocument request) 
			throws SAMLServerException
	{
		SAMLExternalLogoutContext externalCtx = initFromSAML(request, null, Binding.SOAP, false);
		SAMLInternalLogoutContext internalCtx = new SAMLInternalLogoutContext(externalCtx.getSession(), 
				externalCtx.getLocalSessionAuthorityId(), 
				request.getLogoutRequest().getIssuer().getStringValue());
		logoutSynchronousParticipants(internalCtx);
		boolean allLoggedOut = internalCtx.getFailed().isEmpty();
		sessionManagement.removeSession(internalCtx.getSession().getId(), false);
		LogoutResponseDocument finalResponse = prepareFinalLogoutResponse(externalCtx, !allLoggedOut);
		return finalResponse;
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
	public void handleAsyncLogout(LogoutRequestDocument request, String relayState, 
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
		
		SAMLInternalLogoutContext internalCtx = new SAMLInternalLogoutContext(externalCtx.getSession(), 
				externalCtx.getLocalSessionAuthorityId(), 
				request.getLogoutRequest().getIssuer().getStringValue());
		contextsStore.addInternalContext(externalCtx.getRequestersRelayState(), internalCtx);
		
		continueAsyncLogout(internalCtx, response);
	}
	
	/**
	 * Handles Logout response which can be received during the process of asynchronous logout, 
	 * initiated by {@link #handleAsyncLogout(LogoutRequestDocument, HttpServletResponse)}. This method 
	 * process the response, updated the state of the logout process and continues it. 
	 * @param samlResponse
	 * @param state
	 * @param response
	 * @throws EopException 
	 * @throws IOException 
	 */
	public void handleAsyncLogoutResponse(LogoutResponseDocument samlResponse, String state, 
			HttpServletResponse response) throws IOException, EopException
	{
		if (state == null)
		{
			responseHandler.showError(new SAMLProcessingException("A logout response "
					+ "was received without relay state. It can not be processed."), response);
			return;
		}
		SAMLInternalLogoutContext ctx = contextsStore.getInternalContext(state);
		if (ctx == null)
		{
			responseHandler.showError(new SAMLProcessingException("A logout response "
					+ "was received with invalid relay state. It can not be processed."), response);
			return;
		}
		
		if (ctx.getCurrent() == null || ctx.getCurrentRequestId() == null)
		{
			responseHandler.showError(new SAMLProcessingException("A logout response "
					+ "was received associated with invalid logout process. "
					+ "It can not be processed."), response);
			return;
		}

		LogoutResponseValidator validator = new LogoutResponseValidator(consumerEndpointUri, 
				ctx.getCurrentRequestId(), trustChecker);
		try
		{
			validator.validate(samlResponse);
		} catch (SAMLValidationException e)
		{
			responseHandler.showError(new SAMLProcessingException("An invalid logout response "
					+ "was received.", e), response);
			return;
		}
		
		String responseIssuer = samlResponse.getLogoutResponse().getIssuer().getStringValue();
		if (!responseIssuer.equals(ctx.getCurrent().getIdentifier()))
		{
			responseHandler.showError(new SAMLProcessingException("An invalid logout response "
					+ "was received - it is not matching the previous request."), response);
			return;
		}
		
		updateContextAfterParicipantLogout(ctx, ctx.getCurrent(), samlResponse);
		
		continueAsyncLogout(ctx, response);
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
	
	/**
	 * Prepares a logout request for the next unprocessed session participant, which is supporting async 
	 * binding. If there is no such participant returns null. If there is a problem creating a request a 
	 * subsequent participant is tried.
	 *   
	 * @param ctx
	 */
	private InterimLogoutRequest selectNextAsyncParticipantForLogout(SAMLInternalLogoutContext ctx) 
	{
		SAMLSessionParticipant participant;
		LogoutRequest request = null;
		do
		{
			participant = findNextForAsyncLogout(ctx);
			if (participant == null)
				return null;
			try
			{
				request = createLogoutRequest(participant);
				break;
			} catch (SAMLResponderException e)
			{
				log.debug("Can not prepare logout request for " + participant, e);
				ctx.getFailed().add(participant);
			}
		} while (request == null);
		
		SAMLEndpointDefinition logoutEndpoint = participant.getLogoutEndpoints().get(Binding.HTTP_POST);
		if (logoutEndpoint == null)
			logoutEndpoint = participant.getLogoutEndpoints().get(Binding.HTTP_REDIRECT);
		ctx.setCurrent(participant);
		ctx.setCurrentRequestId(request.getXMLBean().getID());
		return new InterimLogoutRequest(request.getXMLBeanDoc(), ctx.getRelayState(), logoutEndpoint);
	}
	
	private SAMLSessionParticipant findNextForAsyncLogout(SAMLInternalLogoutContext ctx)
	{
		List<SAMLSessionParticipant> toBeLoggedOut = ctx.getToBeLoggedOut();
		SAMLSessionParticipant participant = null;
		for (int i=0; i<toBeLoggedOut.size(); i++)
		{
			participant = toBeLoggedOut.get(i);
			if (!participant.getLogoutEndpoints().containsKey(Binding.HTTP_POST.toString()) &&
					!participant.getLogoutEndpoints().containsKey(Binding.HTTP_REDIRECT.toString()))
			{
				continue;
			}
			
			toBeLoggedOut.remove(i);
		}
		return participant;
	}

	/**
	 * Takes the next async participant and starts its async logout. If there are no available async participants
	 * then logout of synchronous participants is performed and the final response is returned via redirection/post 
	 * to the original logout requester.
	 * @param ctx
	 * @param response
	 * @throws IOException
	 * @throws EopException 
	 */
	private void continueAsyncLogout(SAMLInternalLogoutContext ctx, HttpServletResponse response) 
			throws IOException, EopException
	{
		InterimLogoutRequest interimReq = selectNextAsyncParticipantForLogout(ctx);
		
		if (interimReq != null)
		{
			responseHandler.sendRequest(interimReq.getEndpoint().getBinding(),
						interimReq.getRequest(), 
						interimReq.getEndpoint().getUrl(), 
						ctx, response);
			return;
		}
		
		logoutSynchronousParticipants(ctx);

		contextsStore.removeInternalContext(ctx.getRelayState());
		sessionManagement.removeSession(ctx.getSession().getId(), false);
		
		SAMLExternalLogoutContext externalCtx = contextsStore.getExternalContext(ctx.getRelayState());
		if (externalCtx != null)
			finishAsyncLogoutFromSAML(externalCtx, !ctx.getFailed().isEmpty(), 
					response, ctx.getRelayState());
	}

	/**
	 * Prepares the final response and sends it back via async binding
	 * @throws EopException 
	 * @throws IOException 
	 */
	private void finishAsyncLogoutFromSAML(SAMLExternalLogoutContext ctx, boolean partial, 
			HttpServletResponse response, String externalContextKey) throws IOException, EopException
	{
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
	 * Logs out all unprocessed participants who support soap binding.
	 * @param ctx
	 * @throws SAMLResponderException 
	 */
	private void logoutSynchronousParticipants(SAMLInternalLogoutContext ctx)
	{
		List<SAMLSessionParticipant> toBeLoggedOut = ctx.getToBeLoggedOut();
		SAMLSessionParticipant participant = null;
		for (int i=0; i<toBeLoggedOut.size(); i++)
		{
			participant = toBeLoggedOut.get(i);
			SAMLEndpointDefinition soapLogoutEndpoint = participant.getLogoutEndpoints().get(Binding.SOAP);
			if (soapLogoutEndpoint == null)
			{
				continue;
			}
			
			toBeLoggedOut.remove(i);
			
			try
			{
				LogoutRequest logoutRequest = createLogoutRequest(participant);
				SAMLLogoutClient client = new SAMLLogoutClient(soapLogoutEndpoint.getUrl(), 
						soapClientConfig);
				LogoutResponseDocument resp = client.logout(logoutRequest.getXMLBeanDoc());
				updateContextAfterParicipantLogout(ctx, participant, resp);
			} catch (Exception e)
			{
				log.debug("Logging out the participant " + participant + " via SOAP failed", e);
				ctx.getFailed().add(participant);
			}
		}
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
		LogoutResponse response = new LogoutResponse(getIssuer(), ctx.getRequest().getID());
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
				trustChecker, requestValidity, replayChecker);
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
	
	private NameIDType getIssuer()
	{
		return new NameID(localSamlId, SAMLConstants.NFORMAT_ENTITY).getXBean();
	}
	
	private LogoutRequest createLogoutRequest(SAMLSessionParticipant participant) throws SAMLResponderException
	{
		String toBeLoggedOut = participant.getPrincipalNameAtParticipant();
		NameIDType toBeLoggedOutXml;
		try
		{
			toBeLoggedOutXml = NameIDType.Factory.parse(toBeLoggedOut);
		} catch (XmlException e)
		{
			log.error("Can't parse a stored logged user's identity", e);
			throw new SAMLResponderException("Internal error");
		}
		LogoutRequest request = new LogoutRequest(getIssuer(), toBeLoggedOutXml);
		request.setNotAfter(new Date(System.currentTimeMillis() + requestValidity));
		request.setSessionIds(participant.getSessionIndex());
		try
		{
			request.sign(localSamlCredential.getKey(), localSamlCredential.getCertificateChain());
		} catch (DSigException e)
		{
			log.warn("Unable to sign SLO request", e);
			throw new SAMLResponderException("Internal server error signing request.");
		}
		return request;
	}
	
	private void updateContextAfterParicipantLogout(SAMLInternalLogoutContext ctx, SAMLSessionParticipant participant,
			LogoutResponseDocument resp)
	{
		StatusType status = resp.getLogoutResponse().getStatus();
		if (SAMLConstants.Status.STATUS_OK.equals(status.getStatusCode()))
		{
			log.debug("Successful SOAP logout of participant " + participant);
			ctx.getLoggedOut().add(participant);
		} else
		{
			log.debug("Logging out the participant " + participant + 
					" via SOAP failed, returned status is: " + 
					status.getStatusCode() + " - " + status.getStatusMessage() + 
					" " + status.getStatusDetail());
			ctx.getFailed().add(participant);
		}

		ctx.setCurrent(null);
		ctx.setCurrentRequestId(null);
	}
}
