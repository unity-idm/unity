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

import pl.edu.icm.unity.saml.FreemarkerXHTMLHandler;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import pl.edu.icm.unity.saml.ResponseTemplates;

/**
 * SAML message with metadata which can be sent using either HTTP Redirect or POST bindings.
 * Only suitable for messages which are not signed.
 */
public class SamlRoutableUnsignedMessage implements SamlRoutableMessage
{
	private final XmlObject message;
	private final SAMLMessageType messageType;
	private final String relayState;
	private final String destinationURL;

	public SamlRoutableUnsignedMessage(XmlObject message, SAMLMessageType messageType,
			String relayState, String destinationURL)
	{
		this.message = message;
		this.messageType = messageType;
		this.relayState = relayState;
		this.destinationURL = destinationURL;
	}

	@Override
	public String getPOSTConents(FreemarkerXHTMLHandler handler)
	{
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
	public String getRedirectURL() throws IOException
	{
		return HttpRedirectBindingSupport.getRedirectURL(messageType, relayState, getRawMessage(), 
						destinationURL);
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
		return message.xmlText();
	}
}
