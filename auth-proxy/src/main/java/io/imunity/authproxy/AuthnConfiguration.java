/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;

/**
 * Contains configuration of the authentication subsystem, including maintenance of already authenticated sessions.
 * 
 * TODO prepare actual loading and re-configuration subsystem. 
 */
@Component
public class AuthnConfiguration
{
	public final String clientId;
	public final String clientSecret; 

	public final URI authorizationEndpoint;
	public final URI tokenEndpoint;
	public final URI userInfoEndpoint;
	public final URI responseConsumerAddress;
	public final X509CertChainValidator certValidator;
	
	
	private static final String UNITY_ADDRESS = "https://localhost:2443";
	
	AuthnConfiguration()
	{
		this.clientId = "oauth-client";
		this.clientSecret = "oauth-pass1";
		this.authorizationEndpoint = parseURI(UNITY_ADDRESS + "/oauth2-as/oauth2-authz");
		this.tokenEndpoint = parseURI(UNITY_ADDRESS + "/oauth2/token");
		this.userInfoEndpoint = parseURI(UNITY_ADDRESS + "/oauth2/userinfo");
		this.responseConsumerAddress = parseURI("http://localhost:8000");
		this.certValidator = new BinaryCertChainValidator(true);
	}
	
	private static URI parseURI(String address)
	{
		try
		{
			return new URI(address);
		} catch (URISyntaxException e)
		{
			throw new IllegalArgumentException("Invalid URI: " + address, e);
		}
	}
}
