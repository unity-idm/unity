/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.BaseRemoteASProperties;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.Providers;
import pl.edu.icm.unity.oauth.client.profile.OpenIdProfileFetcher;
import pl.edu.icm.unity.oauth.client.profile.PlainProfileFetcher;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityPropertiesHelper;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

/**
 * Configuration of OAuth client for custom provider.
 * @author K. Benedyczak
 */
public class CustomProviderProperties extends UnityPropertiesHelper implements BaseRemoteASProperties
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, CustomProviderProperties.class);
	
	public enum AccessTokenFormat {standard, httpParams};
	public enum ClientAuthnMode {secretPost, secretBasic};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.client.CLIENT_ID.";
	
	public static final String PROVIDER_TYPE = "type";
	public static final String PROVIDER_LOCATION = "authEndpoint";
	public static final String ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
	public static final String PROVIDER_NAME = "name";
	public static final String SCOPES = "scopes";
	public static final String ACCESS_TOKEN_FORMAT = "accessTokenFormat";
	public static final String OPENID_CONNECT = "openIdConnect";
	public static final String OPENID_DISCOVERY = "openIdConnectDiscoveryEndpoint";
	public static final String ICON_URL = "iconUrl";
	public static final String ADDITIONAL_AUTHZ_PARAMS = "extraAuthzParams.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static 
	{
		
		META.put(PROVIDER_TYPE, new PropertyMD(Providers.custom).
				setDescription("Type of provider. Either a well known provider type can be specified"
						+ " or 'custom'. In the first case only few additional settings are required: "
						+ "client id, secret and translation profile. Other settings as scope "
						+ "can be additionally set to fine tune the remote authentication. "
						+ "In the latter 'custom' case all mandatory options must be set."));
		META.put(PROVIDER_LOCATION, new PropertyMD().
				setDescription("Location (URL) of OAuth2 provider's authorization endpoint. "
						+ "It is mandatory for non OpenID Connect providers, in whose case "
						+ "the endopint can be discovered."));
		META.put(ACCESS_TOKEN_ENDPOINT, new PropertyMD().
				setDescription("Location (URL) of OAuth2 provider's access token endpoint. "
						+ "In case of OpenID Connect mode can be discovered, otherwise mandatory."));
		META.put(PROFILE_ENDPOINT, new PropertyMD().
				setDescription("Location (URL) of OAuth2 provider's user's profile endpoint. "
						+ "It is used to obtain additional user's attributes. "
						+ "It can be autodiscovered for OpenID Connect mode. Otherwise it should be"
						+ " set as otherwise there is bearly no information about the user identity."
						+ " If not set then the only information about the user is the one "
						+ "extracted from the access token (if any)."));
		META.put(PROVIDER_NAME, new PropertyMD().setMandatory().setCanHaveSubkeys().
				setDescription("Name of the OAuth provider to be displayed. Can be localized with locale subkeys."));
		META.put(ICON_URL, new PropertyMD().setCanHaveSubkeys().
				setDescription("URL to provider's logo. Can be http(s), file or data scheme. Can be localized."));
		META.put(CLIENT_ID, new PropertyMD().setMandatory().
				setDescription("Client identifier, obtained during Unity's "
				+ "registration at the provider"));
		META.put(CLIENT_SECRET, new PropertyMD().setSecret().setMandatory().
				setDescription("Client secret, obtained during Unity's "
				+ "registration at the provider"));
		META.put(CLIENT_AUTHN_MODE, new PropertyMD(ClientAuthnMode.secretBasic).
				setDescription("Defines how the client secret and id should be passed to the provider."));
		META.put(SCOPES, new PropertyMD().
				setDescription("Space separated list of authorization scopes to be requested. "
						+ "Most often required if in non OpenID Connect mode, otherwise has a default "
						+ "value of 'openid email'"));
		META.put(ACCESS_TOKEN_FORMAT, new PropertyMD(AccessTokenFormat.standard).
				setDescription("Some providers (Facebook) use legacy format of a response to "
						+ "the access token query. Non standard format can be set here."));
		META.put(OPENID_CONNECT, new PropertyMD("false").
				setDescription("If set to true, then the provider is treated as OpenID "
						+ "Connect 1.0 provider. For such providers specifying " + 
						PROFILE_ENDPOINT + " is not needed as the basic user information "
						+ "is retrieved together with access token. However the " 
						+ "discovery endpoint must be set."));
		META.put(OPENID_DISCOVERY, new PropertyMD().
				setDescription("OpenID Connect Discovery endpoint address, relevant (and required) "
						+ "only when OpenID Connect mode is turned on."));
		META.put(CommonWebAuthnProperties.REGISTRATION_FORM, new PropertyMD().
				setDescription("Registration form to be shown for the locally unknown users which "
						+ "were successfuly authenticated remotely."));
		META.put(CommonWebAuthnProperties.TRANSLATION_PROFILE, new PropertyMD().setMandatory().
				setDescription("Translation profile which will be used to map received user "
						+ "information to a local representation."));
		META.put(CommonWebAuthnProperties.ENABLE_ASSOCIATION, new PropertyMD().
				setDescription("If true then unknown remote user gets an option to associate "
						+ "the remote identity with an another local "
						+ "(already existing) account. Overrides the global setting."));
		META.put(CLIENT_HOSTNAME_CHECKING, new PropertyMD(ServerHostnameCheckingMode.FAIL).
				setDescription("Controls how to react on the DNS name mismatch with "
						+ "the server's certificate. Unless in testing environment "
						+ "should be left on the default setting."));
		META.put(CLIENT_TRUSTSTORE, new PropertyMD().setDescription("Name of the truststore which should be used"
				+ " to validate TLS peer's certificates. "
				+ "If undefined then the system Java tuststore is used."));
		META.put(ADDITIONAL_AUTHZ_PARAMS, new PropertyMD().setList(false).
				setDescription("Allows to specify non-standard, fixed parameters which shall be "
						+ "added to the query string of the authorization redirect request. "
						+ "format must be: PARAM=VALUE"));
	}
	
	private X509CertChainValidator validator = null;
	
	public CustomProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(prefix, properties, META, log);
		boolean openIdConnect = getBooleanValue(OPENID_CONNECT);
		if (openIdConnect)
		{
			if (!isSet(SCOPES))
				setProperty(SCOPES, "openid email");
			if (!isSet(OPENID_DISCOVERY))
				throw new ConfigurationException(getKeyDescription(OPENID_DISCOVERY) + 
						" is mandatory in OpenID Connect mode");
			
		} else
		{
			if (!isSet(PROVIDER_LOCATION))
				throw new ConfigurationException(getKeyDescription(PROVIDER_LOCATION) + 
						" is mandatory in non OpenID Connect mode");
			if (!isSet(ACCESS_TOKEN_ENDPOINT))
				throw new ConfigurationException(getKeyDescription(ACCESS_TOKEN_ENDPOINT) + 
						" is mandatory in non OpenID Connect mode");
		}
		
		if (!isSet(PROVIDER_NAME))
			throw new ConfigurationException(getKeyDescription(PROVIDER_NAME) + 
					" is mandatory");
		
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
	}

	public UserProfileFetcher getUserAttributesResolver()
	{
		boolean openIdConnectMode = getBooleanValue(OPENID_CONNECT);
		return openIdConnectMode ? new OpenIdProfileFetcher() : new PlainProfileFetcher();
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
	
	public List<NameValuePair> getAdditionalAuthzParams()
	{
		List<String> raw = getListOfValues(ADDITIONAL_AUTHZ_PARAMS);
		List<NameValuePair> ret = new ArrayList<>(raw.size());
		for (String rawParam: raw)
		{
			int splitAt = rawParam.indexOf('=');
			if (splitAt == -1)
			{
				log.warn("Specification of extra authz query parameter is invalid, no '=': " + 
						rawParam + " ignoring it");
				continue;
			}
			if (splitAt == rawParam.length()-1)
			{
				log.warn("Specification of extra authz query parameter is invalid, no value: " + 
						rawParam + " ignoring it");
				continue;
			}
			ret.add(new BasicNameValuePair(
					rawParam.substring(0, splitAt), rawParam.substring(splitAt+1)));
		}
		return ret;
	}
	
	public static void setIfUnset(Properties properties, String property, String value)
	{
		if (!properties.containsKey(property))
			properties.setProperty(property, value);
	}
}
