/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.proto.LogoutRequest;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.trust.StrictSamlTrustChecker;
import eu.unicore.samly2.validators.LogoutResponseValidator;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.wsutil.samlclient.SAMLLogoutClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;
import xmlbeans.org.oasis.saml2.protocol.StatusType;

/**
 * Implements handling of the most complicated part of logout process - the logout of SAML session participants. 
 * This class is an engine and can be used in logout triggered via SAML or by other means. 
 * 
 * @author K. Benedyczak
 */
public class InternalLogoutProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, InternalLogoutProcessor.class);
	private static final long DEF_LOGOUT_REQ_VALIDITY = 60000;
	
	private PKIManagement pkiManagement;
	private LogoutContextsStore contextsStore;
	private SLOAsyncResponseHandler responseHandler;
	
	private String consumerEndpointUri;
	

	public InternalLogoutProcessor(PKIManagement pkiManagement,
			LogoutContextsStore contextsStore, SLOAsyncResponseHandler responseHandler,
			String consumerEndpointUri)
	{
		this.pkiManagement = pkiManagement;
		this.contextsStore = contextsStore;
		this.responseHandler = responseHandler;
		this.consumerEndpointUri = consumerEndpointUri;
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
	public void continueAsyncLogout(SAMLInternalLogoutContext ctx, HttpServletResponse response) 
			throws IOException, EopException
	{
		InterimLogoutRequest interimReq = selectNextAsyncParticipantForLogout(ctx);
		
		if (interimReq != null)
		{
			log.debug("Logging out participant in async mode: " + interimReq.getEndpoint());
			responseHandler.sendRequest(interimReq.getEndpoint().getBinding(),
						interimReq.getRequest(), 
						interimReq.getEndpoint().getUrl(), 
						ctx, response);
			return;
		}
		
		logoutSynchronousParticipants(ctx);
		
		log.debug("Async logout process of session peers is completed");
		contextsStore.removeInternalContext(ctx.getRelayState());
		ctx.getFinishCallback().finished(response, ctx);
	}

	/**
	 * Logs out all unprocessed participants who support soap binding.
	 * @param ctx
	 * @throws SAMLResponderException 
	 */
	public void logoutSynchronousParticipants(SAMLInternalLogoutContext ctx)
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
				log.debug("Logging out participant via SOAP: " + participant);
				LogoutRequest logoutRequest = createLogoutRequest(participant, soapLogoutEndpoint);
				IClientConfiguration soapClientConfig = createSoapClientConfig(participant);
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
	 * Handles Logout response which can be received during the process of asynchronous logout, 
	 * initiated by {@link #continueAsyncLogout(SAMLInternalLogoutContext, HttpServletResponse)}. This method 
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

		SamlTrustChecker trustChecker;
		try
		{
			trustChecker = prepareResponseTrustChecker(ctx.getCurrent());
		} catch (EngineException e1)
		{
			responseHandler.showError(new SAMLProcessingException("Internal error - "
				+ "can't establish valid certificates for the session participant", e1), response);
			return;
		}
		
		LogoutResponseValidator validator = new LogoutResponseValidator(consumerEndpointUri, 
				ctx.getCurrentRequestId(), trustChecker);
		try
		{
			validator.validate(samlResponse);
		} catch (SAMLErrorResponseException e)
		{
			//ok - we will handle this accordingly later on
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

	private SamlTrustChecker prepareResponseTrustChecker(SAMLSessionParticipant forWhom) throws EngineException
	{
		Set<String> validSigningCerts = forWhom.getParticipantsCertificates();
		StrictSamlTrustChecker checker = new StrictSamlTrustChecker();
		List<PublicKey> trustedKeys = new ArrayList<PublicKey>();
		for (String certName: validSigningCerts)
		{
			X509Certificate cert = pkiManagement.getCertificate(certName).value;
			trustedKeys.add(cert.getPublicKey());
		}
		checker.addTrustedIssuer(forWhom.getIdentifier(), null, trustedKeys);
		return checker;
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
		SAMLEndpointDefinition logoutEndpoint = null;
		do
		{
			participant = findNextForAsyncLogout(ctx);
			if (participant == null)
				return null;
			logoutEndpoint = participant.getLogoutEndpoints().get(Binding.HTTP_POST);
			if (logoutEndpoint == null)
				logoutEndpoint = participant.getLogoutEndpoints().get(Binding.HTTP_REDIRECT);
			try
			{
				request = createLogoutRequest(participant, logoutEndpoint);
				break;
			} catch (SAMLResponderException e)
			{
				log.debug("Can not prepare logout request for " + participant, e);
				ctx.getFailed().add(participant);
			}
		} while (request == null);
		

		ctx.setCurrent(participant);
		ctx.setCurrentRequestId(request.getXMLBean().getID());
		return new InterimLogoutRequest(request.getXMLBeanDoc(), ctx.getRelayState(), logoutEndpoint);
	}
	
	private SAMLSessionParticipant findNextForAsyncLogout(SAMLInternalLogoutContext ctx)
	{
		List<SAMLSessionParticipant> toBeLoggedOut = ctx.getToBeLoggedOut();
		SAMLSessionParticipant participant;
		for (int i=0; i<toBeLoggedOut.size(); i++)
		{
			participant = toBeLoggedOut.get(i);
			if (!participant.getLogoutEndpoints().containsKey(Binding.HTTP_POST) &&
					!participant.getLogoutEndpoints().containsKey(Binding.HTTP_REDIRECT))
			{
				continue;
			}
			
			toBeLoggedOut.remove(i);
			return participant;
		}
		return null;
	}

	private NameIDType getIssuer(String localSamlId)
	{
		return new NameID(localSamlId, SAMLConstants.NFORMAT_ENTITY).getXBean();
	}
	
	private LogoutRequest createLogoutRequest(SAMLSessionParticipant participant,
			SAMLEndpointDefinition logoutEndpoint) throws SAMLResponderException
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
		LogoutRequest request = new LogoutRequest(getIssuer(participant.getLocalSamlId()), toBeLoggedOutXml);
		request.setNotAfter(new Date(System.currentTimeMillis() + DEF_LOGOUT_REQ_VALIDITY));
		request.setSessionIds(participant.getSessionIndex());
		request.getXMLBean().setDestination(logoutEndpoint.getUrl());
		
		try
		{
			X509Credential credential = pkiManagement.getCredential(participant.getLocalCredentialName());
			request.sign(credential.getKey(), credential.getCertificateChain());
		} catch (DSigException e)
		{
			log.warn("Unable to sign SLO request", e);
			throw new SAMLResponderException("Internal server error signing request.");
		} catch (EngineException e)
		{
			log.warn("Unable to extract credential " + participant.getLocalCredentialName() + 
					" to sign SLO request", e);
			throw new SAMLResponderException("Internal server error signing request.");
		}
		return request;
	}
	
	private void updateContextAfterParicipantLogout(SAMLInternalLogoutContext ctx, SAMLSessionParticipant participant,
			LogoutResponseDocument resp)
	{
		StatusType status = resp.getLogoutResponse().getStatus();
		if (SAMLConstants.Status.STATUS_OK.toString().equals(status.getStatusCode().getValue()))
		{
			log.debug("Successful logout of participant " + participant);
			ctx.getLoggedOut().add(participant);
		} else
		{
			log.debug("Logging out the participant " + participant + 
					" failed, received status is: " + 
					status.getStatusCode().getValue() + " - " + status.getStatusMessage() + 
					" " + status.getStatusDetail());
			ctx.getFailed().add(participant);
		}

		ctx.setCurrent(null);
		ctx.setCurrentRequestId(null);
	}
	
	private IClientConfiguration createSoapClientConfig(SAMLSessionParticipant participant) throws EngineException
	{
		X509Credential credential = pkiManagement.getCredential(participant.getLocalCredentialName());
		IAuthnAndTrustConfiguration defaultTrustConfig = pkiManagement.getMainAuthnAndTrust();
		
		DefaultClientConfiguration ret = new DefaultClientConfiguration(defaultTrustConfig.getValidator(), 
				credential);
		return ret;
	}
}
