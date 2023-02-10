/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeyTypeException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.oidc.metadata.JWKSetRequest;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthJWKSetCache;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthDiscoveryMetadataCache;
import pl.edu.icm.unity.oauth.oidc.metadata.OIDCMetadataRequest;

class IntrospectionServiceContextProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, RemoteTokenIntrospectionService.class);

	private final OAuthDiscoveryMetadataCache oAuthDiscoveryMetadataCache;
	private final OAuthJWKSetCache keyResourceCache;
	private final PKIManagement pkiManagement;

	final Map<String, RemoteIntrospectionServiceContext> remoteConfiguredSerivceContextByIssuer = new HashMap<>();
	final Map<String, RemoteIntrospectionServiceContext> manualyConfiguredServiceContextByIssuer = new HashMap<>();
	final List<TrustedUpstreamConfiguration> config;

	IntrospectionServiceContextProvider(OAuthDiscoveryMetadataCache oAuthDiscoveryMetadataCache,
			OAuthJWKSetCache keyResourceCache, PKIManagement pkiManagement, List<TrustedUpstreamConfiguration> config)

	{
		this.config = config;
		this.oAuthDiscoveryMetadataCache = oAuthDiscoveryMetadataCache;
		this.keyResourceCache = keyResourceCache;
		this.pkiManagement = pkiManagement;

		initManualConfiguredIntrospectionServices(config);
	}

	Optional<RemoteIntrospectionServiceContext> getRemoteServiceContext(String issuer)
	{
		initServicesConfiguredByMetadata(config);
		if (manualyConfiguredServiceContextByIssuer.get(issuer) != null)
			return Optional.of(manualyConfiguredServiceContextByIssuer.get(issuer));
		else if (remoteConfiguredSerivceContextByIssuer.get(issuer) != null)
		{
			return Optional.of(remoteConfiguredSerivceContextByIssuer.get(issuer));
		}
		return Optional.empty();
	}

	private void initManualConfiguredIntrospectionServices(List<TrustedUpstreamConfiguration> trus)
	{
		manualyConfiguredServiceContextByIssuer.clear();
		for (TrustedUpstreamConfiguration trustedUpstreamConfiguration : trus)
		{
			if (!trustedUpstreamConfiguration.isMetadata())
			{
				try
				{
					RemoteIntrospectionServiceContext manualIntrospectionConfig = getManualIntrospectionConfig(trustedUpstreamConfiguration);
					manualyConfiguredServiceContextByIssuer.put(manualIntrospectionConfig.issuer, manualIntrospectionConfig);

				} catch (Exception e)
				{
					log.error("Invalid remote introspection service configuration", e);
				}
			}
		}
	}

	private void initServicesConfiguredByMetadata(List<TrustedUpstreamConfiguration> trus)
	{
		remoteConfiguredSerivceContextByIssuer.clear();
		for (TrustedUpstreamConfiguration trustedUpstreamConfiguration : trus)
		{
			if (trustedUpstreamConfiguration.isMetadata())
			{
				try
				{
					getByMetadataIntrospectionConfig(trustedUpstreamConfiguration)
							.ifPresent(s -> remoteConfiguredSerivceContextByIssuer.put(s.issuer, s));
				} catch (Exception e)
				{
					log.error("Invalid remote introspection service configuration", e);
				}
			}
		}
	}

	private RemoteIntrospectionServiceContext getManualIntrospectionConfig(
			TrustedUpstreamConfiguration trustedUpstreamConfiguration) throws MalformedURLException, JOSEException
	{

		JWSVerifier verifier = getJWSVerifier(getCertificate(trustedUpstreamConfiguration.certificate));
		X509CertChainValidatorExt validator = getValidator(trustedUpstreamConfiguration.clientTrustStore);

		return RemoteIntrospectionServiceContext.builder()
				.withClientId(trustedUpstreamConfiguration.clientId)
				.withClientSecret(trustedUpstreamConfiguration.clientSecret)
				.withIssuer(trustedUpstreamConfiguration.issuerURI)
				.withVerifier(verifier)
				.withUrl(new URL(trustedUpstreamConfiguration.introspectionEndpointURL))
				.withValidator(validator)
				.withHostnameCheckingMode(trustedUpstreamConfiguration.clientHostnameChecking)
				.build();
	}

	private Optional<RemoteIntrospectionServiceContext> getByMetadataIntrospectionConfig(
			TrustedUpstreamConfiguration trustedUpstreamConfiguration)
	{
		X509CertChainValidatorExt validator = getValidator(trustedUpstreamConfiguration.clientTrustStore);

		OIDCProviderMetadata metadata;
		try
		{
			metadata = oAuthDiscoveryMetadataCache.getMetadata(OIDCMetadataRequest.builder()
					.withHostnameChecking(trustedUpstreamConfiguration.clientHostnameChecking)
					.withUrl(trustedUpstreamConfiguration.metadataURL)
					.withValidatorName(trustedUpstreamConfiguration.clientTrustStore)
					.withValidator(validator)
					.build());
		} catch (Exception e)
		{
			log.error("Can not get OIDCMetadata from " + trustedUpstreamConfiguration.metadataURL, e);
			return Optional.empty();
		}

		if (metadata.getJWKSetURI() == null)
		{
			log.debug("JWKSet URI in OIDCMetadata from {} is not provided", trustedUpstreamConfiguration.metadataURL);
			return Optional.empty();
		}

		JWKSet jwkSet;
		try
		{
			jwkSet = keyResourceCache.getMetadata(JWKSetRequest.builder()
					.withHostnameChecking(trustedUpstreamConfiguration.clientHostnameChecking)
					.withUrl(metadata.getJWKSetURI()
							.toURL()
							.toExternalForm())
					.withValidatorName(trustedUpstreamConfiguration.clientTrustStore)
					.withValidator(validator)
					.build());
		} catch (Exception e)
		{
			log.error("Can not get JWKSet from " + metadata.getJWKSetURI(), e);
			return Optional.empty();
		}

		JWSVerifier verifier = null;
		try
		{
			verifier = getJWSVerifier(jwkSet);
		} catch (Exception e)
		{
			log.error("Can not build JWSVerifier from JWKSet", e);
			return Optional.empty();
		}

		try
		{
			return Optional.of(RemoteIntrospectionServiceContext.builder()
					.withClientId(trustedUpstreamConfiguration.clientId)
					.withClientSecret(trustedUpstreamConfiguration.clientSecret)
					.withIssuer(metadata.getIssuer()
							.getValue())
					.withVerifier(verifier)
					.withValidator(validator)
					.withHostnameCheckingMode(trustedUpstreamConfiguration.clientHostnameChecking)
					.withUrl(metadata.getIntrospectionEndpointURI()
							.toURL())
					.build());
		} catch (MalformedURLException e)
		{
			log.error("Invalid remote introspection service configuration", e);
			return Optional.empty();
		}

	}

	private JWSVerifier getJWSVerifier(NamedCertificate certificate) throws JOSEException
	{

		if (certificate.value.getPublicKey() instanceof RSAPublicKey)
		{
			return new RSASSAVerifier((RSAPublicKey) certificate.value.getPublicKey());
		} else if (certificate.value.getPublicKey() instanceof ECPublicKey)
		{
			return new ECDSAVerifier((ECPublicKey) certificate.value.getPublicKey());
		}

		throw new ConfigurationException("Can not build JWSVerifier from certificate " + certificate.name);

	}

	private JWSVerifier getJWSVerifier(JWKSet jwkSet) throws JOSEException
	{
		for (JWK jwk : jwkSet.getKeys())
		{
			if (!jwk.getKeyUse()
					.equals(KeyUse.SIGNATURE))
				continue;

			if (jwk.getKeyType()
					.equals(KeyType.RSA))
			{
				if (!(jwk instanceof RSAKey))
				{
					throw new KeyTypeException(RSAPublicKey.class);
				}
				RSAPublicKey rsaPublicKey = jwk.toRSAKey()
						.toRSAPublicKey();
				return new RSASSAVerifier(rsaPublicKey);
			} else if (jwk.getKeyType()
					.equals(KeyType.EC))
			{
				if (!(jwk instanceof ECKey))
				{
					throw new KeyTypeException(ECPublicKey.class);
				}
				ECPublicKey ecPublicKey = jwk.toECKey()
						.toECPublicKey();
				return new ECDSAVerifier(ecPublicKey);
			}
		}

		throw new ConfigurationException("Can not find JWK key to build verifier");
	}

	private NamedCertificate getCertificate(String certificateName)
	{
		try
		{
			return pkiManagement.getCertificate(certificateName);
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can not establish the certificate " + certificateName, e);
		}
	}

	private X509CertChainValidatorExt getValidator(String validatorName)
	{
		if (validatorName == null)
		{
			return null;
		}

		try
		{
			if (!pkiManagement.getValidatorNames()
					.contains(validatorName))
				throw new ConfigurationException("The http client truststore " + validatorName
						+ " for the OAuth verification client does not exist");
			return pkiManagement.getValidator(validatorName);
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can not establish the http client truststore " + validatorName, e);
		}
	}

}
