/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.saml.FreemarkerXHTMLHandler;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.proto.AbstractSAMLMessage;
import eu.unicore.security.dsig.DSigException;
import pl.edu.icm.unity.saml.ResponseTemplates;

/**
 * SAML message with metadata which can be sent using either HTTP Redirect or POST bindings.
 * Encapsulates crypto material to sign the message - important as the the way in which signature is created 
 * depends on the binding.
 */
public class SamlRoutableSignableMessage<T extends XmlObject> implements SamlRoutableMessage
{
	private final AbstractSAMLMessage<T> message;
	private final X509Credential signingKey;
	private final SAMLMessageType messageType;
	private final String relayState;
	private final String destinationURL;

	public SamlRoutableSignableMessage(AbstractSAMLMessage<T> message, X509Credential signingKey, SAMLMessageType messageType,
			String relayState, String destinationURL)
	{
		this.message = message;
		this.signingKey = signingKey;
		this.messageType = messageType;
		this.relayState = relayState;
		this.destinationURL = destinationURL;
	}

	@Override
	public String getPOSTConents(FreemarkerXHTMLHandler handler) throws DSigException
	{
		if (signingKey != null)
			sign();
		String xmlString = getRawMessage();
		String encodedMessage = Base64.getEncoder().encodeToString(xmlString.getBytes(StandardCharsets.UTF_8));
		Map<String, String> data = new HashMap<>();
		data.put("samlMessage", encodedMessage);
		data.put("messageType", messageType.name());
		data.put("destinationURL", destinationURL);
		if (relayState != null)
			data.put("relayState", relayState);
		
		StringWriter out = new StringWriter();
		try
		{
			handler.printXHTMLDocument(out, ResponseTemplates.POST_BINDING_TMPL.templateFile, data);
		} catch (IOException e)
		{
			throw new RuntimeException("Can't render SAML POST form", e);
		}
		return out.toString();
	}

	@Override
	public String getRedirectURL() throws IOException, DSigException
	{
		String xmlString = getRawMessage();
		return signingKey != null ?
				HttpRedirectBindingSupport.getSignedRedirectURL(messageType, relayState, xmlString, 
						destinationURL, signingKey.getKey()) : 
				HttpRedirectBindingSupport.getRedirectURL(messageType, relayState, xmlString, 
						destinationURL);
	}
	
	public T getSignedMessage() throws DSigException
	{
		if (signingKey != null)
			sign();
		return message.getXMLBeanDoc();
	}

	private void sign() throws DSigException
	{
		message.sign(signingKey.getKey(), signingKey.getCertificateChain());
	}

	@Override
	public SAMLMessageType getMessageType()
	{
		return messageType;
	}

	@Override
	public String getRelayState()
	{
		return relayState;
	}

	@Override
	public String getDestinationURL()
	{
		return destinationURL;
	}
	
	@Override
	public String getRawMessage()
	{
		T xmlDoc = message.getXMLBeanDoc();
		return xmlDoc.xmlText();
	}
}
