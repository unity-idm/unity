package pl.edu.icm.unity.oauth.rp.web;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.jetty.util.StringUtil;
import org.springframework.util.CollectionUtils;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthTokenRepository;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.client.console.OAuthBaseConfiguration;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties.VerificationProtocol;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

public class OAuthRPConfiguration extends OAuthBaseConfiguration
{
	private int cacheTime;
	private VerificationProtocol verificationProtocol;
	private String verificationEndpoint;
	private boolean openIdMode;
	private List<String> requiredScopes;
	private PKIManagement pkiMan;
	private OAuthTokenRepository tokensDAO;

	public OAuthRPConfiguration(PKIManagement pkiMan, OAuthTokenRepository tokensDAO)
	{
		this.pkiMan = pkiMan;
		this.tokensDAO = tokensDAO;
		setVerificationProtocol(VerificationProtocol.unity);
	}

	public void fromProperties(String source)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(source));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth-rp verificator", e);
		}

		OAuthRPProperties oauthRPprop = new OAuthRPProperties(raw, pkiMan, tokensDAO);

		setCacheTime(oauthRPprop.getIntValue(OAuthRPProperties.CACHE_TIME));
		setVerificationProtocol(oauthRPprop.getEnumValue(OAuthRPProperties.VERIFICATION_PROTOCOL,
				VerificationProtocol.class));
		setVerificationEndpoint(oauthRPprop.getValue(OAuthRPProperties.VERIFICATION_ENDPOINT));
		setProfileEndpoint(oauthRPprop.getValue(OAuthRPProperties.PROFILE_ENDPOINT));
		setClientAuthenticationMode(oauthRPprop.getEnumValue(OAuthRPProperties.CLIENT_AUTHN_MODE,
				ClientAuthnMode.class));
		setClientAuthenticationModeForProfile(oauthRPprop.getEnumValue(
				OAuthRPProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, ClientAuthnMode.class));
		setClientHttpMethodForProfileAccess(oauthRPprop.getEnumValue(
				OAuthRPProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
				ClientHttpMethod.class));
		setRequiredScopes(oauthRPprop.getListOfValues(OAuthRPProperties.REQUIRED_SCOPES).stream()
				.filter(StringUtil::isNotBlank)
				.collect(Collectors.toList()));
		
		setClientId(oauthRPprop.getValue(OAuthRPProperties.CLIENT_ID));
		setClientSecret(oauthRPprop.getValue(OAuthRPProperties.CLIENT_SECRET));
		setOpenIdMode(oauthRPprop.getBooleanValue(OAuthRPProperties.OPENID_MODE));

		setClientHostnameChecking(oauthRPprop.getEnumValue(OAuthRPProperties.CLIENT_HOSTNAME_CHECKING,
				ServerHostnameCheckingMode.class));
		setClientTrustStore(oauthRPprop.getValue(OAuthRPProperties.CLIENT_TRUSTSTORE));

		if (oauthRPprop.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
		{
			setTranslationProfile(TranslationProfileGenerator.getProfileFromString(oauthRPprop
					.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE)));

		} else
		{
			setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(
					oauthRPprop.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE)));
		}
	}

	public String toProperties() throws ConfigurationException
	{
		Properties raw = new Properties();
		raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_ID, getClientId());
		raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_SECRET, getClientSecret());

		if (!CollectionUtils.isEmpty(requiredScopes))
		{
			for (int i = 0; i < requiredScopes.size(); i++)
			{
				String scope = requiredScopes.get(i);
				if (StringUtil.isNotBlank(scope))
				{
					raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.REQUIRED_SCOPES + (i+1), scope.trim());
				}
			}
		}

		raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.OPENID_MODE, String.valueOf(openIdMode));

		if (getVerificationEndpoint() != null)
		{
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.VERIFICATION_ENDPOINT,
					getVerificationEndpoint());
		}

		if (verificationProtocol != null)
		{
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.VERIFICATION_PROTOCOL,
					verificationProtocol.toString());
		}

		if (getProfileEndpoint() != null)
		{
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.PROFILE_ENDPOINT,
					getProfileEndpoint());
		}

		raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CACHE_TIME, String.valueOf(cacheTime));

		if (getClientAuthenticationMode() != null)
		{
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_AUTHN_MODE,
					getClientAuthenticationMode().toString());
		}

		if (getClientAuthenticationModeForProfile() != null)
		{
			raw.put(OAuthRPProperties.PREFIX
					+ OAuthRPProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS,
					getClientAuthenticationModeForProfile().toString());
		}

		if (getClientHttpMethodForProfileAccess() != null)
		{
			raw.put(OAuthRPProperties.PREFIX
					+ OAuthRPProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
					getClientHttpMethodForProfileAccess().toString());
		}

		try
		{
			raw.put(OAuthRPProperties.PREFIX
					+ CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
					Constants.MAPPER.writeValueAsString(
							getTranslationProfile().toJsonObject()));
		} catch (Exception e)
		{
			throw new InternalException("Can't serialize authenticator translation profile to JSON",
					e);
		}

		if (getClientHostnameChecking() != null)
		{
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_HOSTNAME_CHECKING,
					getClientHostnameChecking().toString());
		}

		if (getClientTrustStore() != null)
		{
			raw.put(OAuthRPProperties.PREFIX + OAuthRPProperties.CLIENT_TRUSTSTORE,
					getClientTrustStore());
		}

		OAuthRPProperties prop = new OAuthRPProperties(raw, pkiMan, tokensDAO);
		return prop.getAsString();

	}

	public int getCacheTime()
	{
		return cacheTime;
	}

	public void setCacheTime(int cacheTime)
	{
		this.cacheTime = cacheTime;
	}

	public VerificationProtocol getVerificationProtocol()
	{
		return verificationProtocol;
	}

	public void setVerificationProtocol(VerificationProtocol verificationProtocol)
	{
		this.verificationProtocol = verificationProtocol;
	}

	public String getVerificationEndpoint()
	{
		return verificationEndpoint;
	}

	public void setVerificationEndpoint(String verificationEndpoint)
	{
		this.verificationEndpoint = verificationEndpoint;
	}

	public boolean isOpenIdMode()
	{
		return openIdMode;
	}

	public void setOpenIdMode(boolean openIdMode)
	{
		this.openIdMode = openIdMode;
	}

	public List<String> getRequiredScopes()
	{
		return requiredScopes;
	}

	public void setRequiredScopes(List<String> requiredScopes)
	{
		this.requiredScopes = requiredScopes;
	}
}