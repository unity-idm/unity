/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import org.apache.xmlbeans.XmlObject;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.binding.HttpPostBindingSupport;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.proto.AbstractSAMLMessage;
import eu.unicore.security.dsig.DSigException;

/**
 * SAML message with metadata which can be sent using either HTTP Redirect or POST bindings.
 * Encapsulates crypto material to sign the message - important as the the way in which signature is created 
 * depends on the binding.
 */
class SamlMessageSpec<T extends XmlObject>
{
	private final AbstractSAMLMessage<T> message;
	private final X509Credential signingKey;
	private final SAMLMessageType messageType;
	private final String relayState;
	private final String samlParticipantURL;

	SamlMessageSpec(AbstractSAMLMessage<T> message, X509Credential signingKey, SAMLMessageType messageType,
			String relayState, String samlParticipantURL)
	{
		this.message = message;
		this.signingKey = signingKey;
		this.messageType = messageType;
		this.relayState = relayState;
		this.samlParticipantURL = samlParticipantURL;
	}

	String getPOSTConents() throws DSigException
	{
		if (signingKey != null)
			sign();
		String xmlString = getRawMessage();
		return HttpPostBindingSupport.getHtmlPOSTFormContents(
				messageType, samlParticipantURL, xmlString, relayState);
	}

	String getRedirectURL() throws IOException, DSigException
	{
		String xmlString = getRawMessage();
		return signingKey != null ?
				HttpRedirectBindingSupport.getSignedRedirectURL(messageType, relayState, xmlString, 
						samlParticipantURL, signingKey.getKey()) : 
				HttpRedirectBindingSupport.getRedirectURL(messageType, relayState, xmlString, 
						samlParticipantURL);
	}
	
	T getSignedMessage() throws DSigException
	{
		if (signingKey != null)
			sign();
		return message.getXMLBeanDoc();
	}

	private void sign() throws DSigException
	{
		message.sign(signingKey.getKey(), signingKey.getCertificateChain());
	}

	SAMLMessageType getMessageType()
	{
		return messageType;
	}

	String getRelayState()
	{
		return relayState;
	}

	String getSamlParticipantURL()
	{
		return samlParticipantURL;
	}
	
	String getRawMessage()
	{
		T xmlDoc = message.getXMLBeanDoc();
		return xmlDoc.xmlText();
	}
}
