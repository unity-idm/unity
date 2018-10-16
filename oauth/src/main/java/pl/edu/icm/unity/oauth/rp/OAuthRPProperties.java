/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.BaseRemoteASProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.rp.verificator.InternalTokenVerificator;
import pl.edu.icm.unity.oauth.rp.verificator.MitreTokenVerificator;
import pl.edu.icm.unity.oauth.rp.verificator.TokenVerificatorProtocol;
import pl.edu.icm.unity.oauth.rp.verificator.UnityTokenVerificator;

/**
 * Configuration of OAuth RP-alike authenticator.
 * @author K. Benedyczak
 */
public class OAuthRPProperties extends PropertiesHelper implements BaseRemoteASProperties
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, OAuthRPProperties.class);
	
	public enum VerificationProtocol {mitre, unity, internal};
	
	public static final int DEFAULT_CACHE_TTL = 60;
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.oauth2-rp.";
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META=new HashMap<String, PropertyMD>();
	
	public static final String CACHE_TIME = "cacheTime";
	public static final String VERIFICATION_PROTOCOL = "verificationProtocol";
	public static final String VERIFICATION_ENDPOINT = "verificationEndpoint";
	public static final String OPENID_MODE = "openidConnectMode";
	public static final String OPENID_MODE_WITH_TYPO = "opeinidConnectMode";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	public static final String REQUIRED_SCOPES = "requiredScopes.";
	
	static
	{
		META.put(CACHE_TIME, new PropertyMD().setInt().setNonNegative().
				setDescription("Per-token validation result cache time in seconds. "
						+ "If unset then the cache time will be equal "
						+ "to the discovered token lifetime or to " + DEFAULT_CACHE_TTL
						+ "s if it is impossible to establish the lifetime. "
						+ "Set to zero to disable caching."));
		META.put(PROFILE_ENDPOINT, new PropertyMD().
				setDescription("Location (URL) of OAuth2 provider's user's profile endpoint. "
						+ "It is used to obtain token issuer's attributes."));
		META.put(VERIFICATION_PROTOCOL, new PropertyMD(VerificationProtocol.unity).
				setDescription("OAuth token verification is not standardised. "
						+ "Unity supports several protocols, you can set the proper one here."));
		META.put(VERIFICATION_ENDPOINT, new PropertyMD().
				setDescription("OAuth token verification endpoint address."));
		META.put(CLIENT_ID, new PropertyMD().
				setDescription("Client identifier, used to authenticate when performing validation. "
						+ "If not defined then only the access token is used to "
						+ "authorize the call."));
		META.put(CLIENT_SECRET, new PropertyMD().setSecret().setMandatory().
				setDescription("Client secret,  used to authenticate when performing validation. "
						+ "If not defined then only the access token is used to "
						+ "authorize the call."));
		META.put(CLIENT_AUTHN_MODE, new PropertyMD(ClientAuthnMode.secretBasic).
				setDescription("Defines how the client access token should be passed to the AS."));
		META.put(CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, new PropertyMD().setDescription(
				"Defines how the client secret and id should be passed to the provider's user's profile endpoint. If not set the "
						+ CLIENT_AUTHN_MODE + " is used"));
		META.put(CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS, new PropertyMD(ClientHttpMethod.get)
				.setDescription("Http method used in query to profile endpoint"));
		META.put(REQUIRED_SCOPES, new PropertyMD().setList(false).
				setDescription("Optional list of scopes which must be associated with the validated"
						+ " access token to make the authentication successful"));
		META.put(OPENID_MODE, new PropertyMD("false").
				setDescription("If true then the profile is fetched from the profile endpoint"
						+ " with assumption that the server is working in the OpenID Connect "
						+ "compatible way."));
		META.put(OPENID_MODE_WITH_TYPO, new PropertyMD("false").setDeprecated().
				setDescription("Use the option without type - this one is provided for backwards "
						+ "compatibility only."));
		META.put(CLIENT_HOSTNAME_CHECKING, new PropertyMD(ServerHostnameCheckingMode.FAIL).
				setDescription("Controls how to react on the DNS name mismatch with "
						+ "the server's certificate. Unless in testing environment "
						+ "should be left on the default setting."));
		META.put(CLIENT_TRUSTSTORE, new PropertyMD().setDescription("Name of the truststore which should be used"
				+ " to validate TLS peer's certificates. "
				+ "If undefined then the system Java tuststore is used."));
		META.put(TRANSLATION_PROFILE, new PropertyMD().setMandatory().setDescription(
				"Name of a translation" +
				" profile, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));

	}
	
	private X509CertChainValidator validator = null;
	private TokensManagement tokensMan;
	
	public OAuthRPProperties(Properties properties, PKIManagement pkiManagement,
			TokensManagement tokensMan) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
		this.tokensMan = tokensMan;
		String validatorName = getValue(CLIENT_TRUSTSTORE);
		if (validatorName != null)
		{
			try
			{
				if (!pkiManagement.getValidatorNames().contains(validatorName))
					throw new ConfigurationException("The validator " + 
							validatorName + 
							" for the OAuth verification client does not exist");
				validator = pkiManagement.getValidator(validatorName);
			} catch (EngineException e)
			{
				throw new ConfigurationException("Can not establish the validator " + 
						validatorName + " for the OAuth verification client", e);
			}
		}
		VerificationProtocol proto = getEnumValue(VERIFICATION_PROTOCOL, VerificationProtocol.class);
		if (proto != VerificationProtocol.internal && !isSet(VERIFICATION_ENDPOINT))
			throw new ConfigurationException("The " + getKeyDescription(VERIFICATION_ENDPOINT) +
					" property is mandatory unless the '" + VerificationProtocol.internal +
					"' verification protocol is used");
	}
	
	@Override
	public ClientAuthnMode getClientAuthModeForProfileAccess()
	{
		ClientAuthnMode mode = getEnumValue(CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS,
				ClientAuthnMode.class);
		return mode != null ? mode : getEnumValue(CLIENT_AUTHN_MODE, ClientAuthnMode.class);
	}
	
	public Method getClientHttpMethodForProfileAccess()
	{
		return (getEnumValue(CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
				ClientHttpMethod.class) == ClientHttpMethod.get) ? Method.GET
						: Method.POST;
	}	
	
	public boolean isSetOpenIdMode()
	{
		return isSet(OPENID_MODE) ? getBooleanValue(OPENID_MODE) : getBooleanValue(OPENID_MODE_WITH_TYPO);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
	
	@Override
	public X509CertChainValidator getValidator()
	{
		return validator;
	}
	
	public TokenVerificatorProtocol getTokenChecker()
	{
		VerificationProtocol proto = getEnumValue(VERIFICATION_PROTOCOL, VerificationProtocol.class);
		switch (proto)
		{
		case mitre:
			return new MitreTokenVerificator(this);
		case unity:
			return new UnityTokenVerificator(this);
		case internal:
			return new InternalTokenVerificator(tokensMan);
		}
		throw new IllegalStateException("Bug: unhandled protocol");
	}
}
