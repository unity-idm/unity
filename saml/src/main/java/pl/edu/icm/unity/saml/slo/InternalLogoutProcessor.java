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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.messages.SAMLMessage;
import eu.unicore.samly2.proto.LogoutRequest;
import eu.unicore.samly2.slo.LogoutResponseValidator;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.wsutil.samlclient.SAMLLogoutClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.IClientConfiguration;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;
import xmlbeans.org.oasis.saml2.protocol.StatusType;

/**
 * Implements handling of the most complicated part of logout process - the logout of SAML session participants. 
 * This class is an engine and can be used in logout triggered via SAML or by other means. 
 * 
 * @author K. Benedyczak
 */
class InternalLogoutProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, InternalLogoutProcessor.class);
	private static final long DEF_LOGOUT_REQ_VALIDITY = 60000;
	
	private PKIManagement pkiManagement;
	private LogoutContextsStore contextsStore;
	private SLOAsyncMessageHandler responseHandler;
	
	private String consumerEndpointUri;
	

	InternalLogoutProcessor(PKIManagement pkiManagement,
			LogoutContextsStore contextsStore, SLOAsyncMessageHandler responseHandler,
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
	 */
	void continueAsyncLogout(SAMLInternalLogoutContext ctx, HttpServletResponse response) 
			throws IOException, EopException
	{
		InterimLogoutRequest interimReq = selectNextAsyncParticipantForLogout(ctx);
		
		if (interimReq != null)
		{
			log.info("Logging out participant in async mode: {}", interimReq.endpoint);
			sendRequest(interimReq, response);
			return;
		}
		
		logoutSynchronousParticipants(ctx);
		
		log.info("Async logout process of session peers is completed");
		contextsStore.removeInternalContext(ctx.getRelayState());
		ctx.getFinishCallback().finished(response, ctx);
	}

	private void sendRequest(InterimLogoutRequest interimReq, HttpServletResponse response) throws IOException, EopException
	{
		try
		{
			responseHandler.sendRequest(interimReq.endpoint.getBinding(), interimReq.request, response);
		} catch (DSigException e)
		{
			log.error("Can't sign SLO request to subsequent part, likely configuration problem", e);
			throw new IOException("Error signing SLO request to subsequent peer", e);
		}
	}
	
	/**
	 * Logs out all unprocessed participants who support soap binding.
	 */
	void logoutSynchronousParticipants(SAMLInternalLogoutContext ctx)
	{
		Iterator<SAMLSessionParticipant> toBeLoggedOut = ctx.getToBeLoggedOut().iterator();
		SAMLSessionParticipant participant = null;
		while (toBeLoggedOut.hasNext())
		{
			participant = toBeLoggedOut.next();
			SAMLEndpointDefinition soapLogoutEndpoint = participant.getLogoutEndpoints().get(Binding.SOAP);
			if (soapLogoutEndpoint == null)
				continue;
			
			toBeLoggedOut.remove();
			
			try
			{
				log.info("Logging out participant via SOAP: " + participant);
				LogoutRequest logoutRequest = createSignedLogoutRequest(participant, soapLogoutEndpoint);
				IClientConfiguration soapClientConfig = createSoapClientConfig(participant);
				SAMLLogoutClient client = new SAMLLogoutClient(soapLogoutEndpoint.getUrl(), 
						soapClientConfig);
				LogoutResponseDocument resp = client.logout(logoutRequest.getXMLBeanDoc());
				updateContextAfterParicipantLogout(ctx, participant, resp);
			} catch (Exception e)
			{
				log.warn("Logging out the participant " + participant + " via SOAP failed", e);
				ctx.getFailed().add(participant);
			}
		}
	}
	
	
	/**
	 * Handles Logout response which can be received during the process of asynchronous logout, 
	 * initiated by {@link #continueAsyncLogout(SAMLInternalLogoutContext, HttpServletResponse)}. This method 
	 * process the response, updated the state of the logout process and continues it.
	 */
	void handleAsyncLogoutResponse(SAMLMessage<LogoutResponseDocument> samlMessage, 
			HttpServletResponse response) throws IOException, EopException
	{
		if (samlMessage.relayState == null)
		{
			responseHandler.showError(new SAMLProcessingException("A logout response "
					+ "was received without relay state. It can not be processed."), response);
			return;
		}
		SAMLInternalLogoutContext ctx = contextsStore.getInternalContext(samlMessage.relayState);
		if (ctx == null)
		{
			responseHandler.showError(new SAMLProcessingException("A logout response "
					+ "was received with invalid relay state. It can not be processed."), response);
			return;
		}
		
		SAMLSessionParticipant currentParticipant = ctx.getCurrent();
		if (currentParticipant == null || ctx.getCurrentRequestId() == null)
		{
			responseHandler.showError(new SAMLProcessingException("A logout response "
					+ "was received associated with invalid logout process. "
					+ "It can not be processed."), response);
			return;
		}

		List<PublicKey> validKeys;
		try
		{
			validKeys = getSigningKeysForCurrentParticipant(currentParticipant);
		} catch (EngineException e1)
		{
			responseHandler.showError(new SAMLProcessingException("Internal error - "
				+ "can't establish valid signing keys for the session participant", e1), response);
			return;
		}
		
		LogoutResponseValidator validator = new LogoutResponseValidator(consumerEndpointUri, 
				ctx.getCurrentRequestId(), 
				participant -> getSigningKeysForGivenParticipant(
						currentParticipant.getIdentifier(), validKeys, participant));
		try
		{
			validator.validate(samlMessage.messageDocument, samlMessage.verifiableMessage);
		} catch (SAMLErrorResponseException e)
		{
			//ok - we will handle this accordingly later on
		} catch (SAMLValidationException e)
		{
			responseHandler.showError(new SAMLProcessingException("An invalid logout response "
					+ "was received.", e), response);
			return;
		}
		
		String responseIssuer = samlMessage.messageDocument.getLogoutResponse().getIssuer().getStringValue();
		if (!responseIssuer.equals(currentParticipant.getIdentifier()))
		{
			responseHandler.showError(new SAMLProcessingException("An invalid logout response "
					+ "was received - it is not matching the previous request."), response);
			return;
		}
		
		updateContextAfterParicipantLogout(ctx, currentParticipant, samlMessage.messageDocument);
		
		continueAsyncLogout(ctx, response);
	}

	private List<PublicKey> getSigningKeysForCurrentParticipant(SAMLSessionParticipant forWhom) throws EngineException
	{
		Set<String> validSigningCerts = forWhom.getParticipantsCertificates();
		List<PublicKey> trustedKeys = new ArrayList<>();
		for (String certName: validSigningCerts)
		{
			X509Certificate cert = pkiManagement.getCertificate(certName).value;
			trustedKeys.add(cert.getPublicKey());
		}
		return trustedKeys;
	}
	
	private List<PublicKey> getSigningKeysForGivenParticipant(String currentParticipantEntityId, List<PublicKey> validKeys,
			NameIDType participantId)
	{
		if (!currentParticipantEntityId.equals(participantId.getStringValue()) 
				|| (participantId.getFormat() != null && !participantId.getFormat().equals(SAMLConstants.NFORMAT_ENTITY)))
			return null;
		return validKeys;
	}
	
	/**
	 * Prepares a logout request for the next unprocessed session participant, which is supporting async 
	 * binding. If there is no such participant returns null. If there is a problem creating a request a 
	 * subsequent participant is tried.
	 */
	private InterimLogoutRequest selectNextAsyncParticipantForLogout(SAMLInternalLogoutContext ctx) throws IOException
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
			if (logoutEndpoint == null)
			{
				log.warn("Can not prepare logout request for {} - no logout endpoint", participant);
				ctx.getFailed().add(participant);
				continue;
			}
			try
			{
				request = createLogoutRequest(participant, logoutEndpoint);
				break;
			} catch (SAMLResponderException e)
			{
				log.warn("Can not prepare logout request for " + participant, e);
				ctx.getFailed().add(participant);
			}
		} while (request == null);
		

		ctx.setCurrent(participant);
		ctx.setCurrentRequestId(request.getXMLBean().getID());
		X509Credential credential;
		try
		{
			credential = pkiManagement.getCredential(participant.getLocalCredentialName());
		} catch (EngineException e)
		{
			log.error("Can't get credential for signing request", e);
			throw new IOException("Can't get credential for signing request", e);
		}
		SamlRoutableSignableMessage<LogoutRequestDocument> samlMessageSpec = new SamlRoutableSignableMessage<>(request, credential, 
				SAMLMessageType.SAMLRequest, ctx.getRelayState(), logoutEndpoint.getUrl());
		return new InterimLogoutRequest(samlMessageSpec, logoutEndpoint);
	}
	
	private X509Credential getCredential(String credentialName) throws SAMLResponderException
	{
		try
		{
			return pkiManagement.getCredential(credentialName);
		} catch (EngineException e)
		{
			log.warn("Unable to extract credential {} to sign SLO request", credentialName, e);
			throw new SAMLResponderException("Internal server error signing request.");
		}
	}
	
	private SAMLSessionParticipant findNextForAsyncLogout(SAMLInternalLogoutContext ctx)
	{
		Iterator<SAMLSessionParticipant> toBeLoggedOut = ctx.getToBeLoggedOut().iterator();
		SAMLSessionParticipant participant;
		while (toBeLoggedOut.hasNext())
		{
			participant = toBeLoggedOut.next();
			if (!participant.getLogoutEndpoints().containsKey(Binding.HTTP_POST) &&
					!participant.getLogoutEndpoints().containsKey(Binding.HTTP_REDIRECT))
			{
				continue;
			}
			
			toBeLoggedOut.remove();
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
		return request;
	}
	
	private LogoutRequest createSignedLogoutRequest(SAMLSessionParticipant participant,
			SAMLEndpointDefinition logoutEndpoint) throws SAMLResponderException
	{
		LogoutRequest request = createLogoutRequest(participant, logoutEndpoint);
		X509Credential credential = getCredential(participant.getLocalCredentialName());
		try
		{
			request.sign(credential.getKey(), credential.getCertificateChain());
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
		if (SAMLConstants.Status.STATUS_OK.toString().equals(status.getStatusCode().getValue()))
		{
			log.info("Successful logout of participant " + participant);
			ctx.getLoggedOut().add(participant);
		} else
		{
			log.warn("Logging out the participant " + participant + 
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
	
	
	/**
	 * Holds a Logout Request along with its destination and relay state. This is used to pass necessary information
	 * to perform an in-the-middle request to a session participant in asynchronous bindings.
	 * @author K. Benedyczak
	 */
	private static class InterimLogoutRequest
	{
		final SamlRoutableSignableMessage<LogoutRequestDocument> request;
		final SAMLEndpointDefinition endpoint;
		
		InterimLogoutRequest(SamlRoutableSignableMessage<LogoutRequestDocument> request, SAMLEndpointDefinition endpoint)
		{
			this.request = request;
			this.endpoint = endpoint;
		}
	}
}
