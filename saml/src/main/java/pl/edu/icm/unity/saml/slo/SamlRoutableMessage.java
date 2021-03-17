/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.security.dsig.DSigException;

public interface SamlRoutableMessage
{
	String getPOSTConents() throws DSigException;

	String getRedirectURL() throws IOException, DSigException;

	SAMLMessageType getMessageType();

	String getRelayState();

	String getDestinationURL();

	String getRawMessage();
}