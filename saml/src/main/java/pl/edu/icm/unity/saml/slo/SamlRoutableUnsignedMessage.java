/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import org.apache.xmlbeans.XmlObject;

import eu.unicore.samly2.binding.HttpPostBindingSupport;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;

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
	public String getPOSTConents()
	{
		return HttpPostBindingSupport.getHtmlPOSTFormContents(
				messageType, destinationURL, getRawMessage(), relayState);
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
