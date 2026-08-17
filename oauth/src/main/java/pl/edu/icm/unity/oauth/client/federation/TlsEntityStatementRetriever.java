/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.net.URI;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.federation.api.FetchEntityStatementRequest;
import com.nimbusds.openid.connect.sdk.federation.api.FetchEntityStatementSuccessResponse;
import com.nimbusds.openid.connect.sdk.federation.config.FederationEntityConfigurationRequest;
import com.nimbusds.openid.connect.sdk.federation.config.FederationEntityConfigurationSuccessResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.EntityStatementRetriever;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

public class TlsEntityStatementRetriever implements EntityStatementRetriever
{
	private final X509CertChainValidator validator;
	private final ServerHostnameCheckingMode hostnameCheckingMode;
	private final HttpRequestConfigurer configurer = new HttpRequestConfigurer();

	public TlsEntityStatementRetriever(X509CertChainValidator validator,
			ServerHostnameCheckingMode hostnameCheckingMode)
	{
		this.validator = validator;
		this.hostnameCheckingMode = hostnameCheckingMode;
	}

	@Override
	public EntityStatement fetchEntityConfiguration(EntityID entityID) throws ResolveException
	{
		try
		{
			HTTPRequest httpRequest = new FederationEntityConfigurationRequest(entityID).toHTTPRequest();
			configurer.secureRequest(httpRequest, validator, hostnameCheckingMode);
			HTTPResponse response = httpRequest.send();
			return FederationEntityConfigurationSuccessResponse.parse(response).getEntityStatement();
		} catch (Exception e)
		{
			throw new ResolveException("Failed to fetch entity configuration for " + entityID, e);
		}
	}

	@Override
	public EntityStatement fetchEntityStatement(URI federationApiURI, EntityID issuer, EntityID subject)
			throws ResolveException
	{
		try
		{
			HTTPRequest httpRequest = new FetchEntityStatementRequest(federationApiURI, issuer, subject)
					.toHTTPRequest();
			configurer.secureRequest(httpRequest, validator, hostnameCheckingMode);
			HTTPResponse response = httpRequest.send();
			return FetchEntityStatementSuccessResponse.parse(response).getEntityStatement();
		} catch (Exception e)
		{
			throw new ResolveException(
					"Failed to fetch entity statement from " + federationApiURI + " for " + subject, e);
		}
	}
}
