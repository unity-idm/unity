/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.hc.core5.http.NameValuePair;

import com.google.common.base.Strings;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.Providers;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.binding.NameValuePairBinding;
import pl.edu.icm.unity.webui.common.binding.ToggleWithDefault;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

public class OAuthProviderConfiguration extends OAuthBaseConfiguration
{
	private String type;
	private String id;
	private I18nString name;
	private LocalOrRemoteResource logo;
	private boolean openIdConnect;
	private String openIdDiscoverEndpoint;
	private String authenticationEndpoint;
	private String accessTokenEndpoint;
	private String registrationForm;
	private AccessTokenFormat accessTokenFormat;
	private ToggleWithDefault accountAssociation;
	private List<NameValuePairBinding> extraAuthorizationParameters;
	private List<String> requestedScopes;

	public OAuthProviderConfiguration()
	{
		super();
		setType(Providers.custom.toString());
		setAccessTokenFormat(AccessTokenFormat.standard);
		extraAuthorizationParameters = new ArrayList<>();
		accountAssociation = ToggleWithDefault.bydefault;
	}

	public void fromTemplate(MessageSource msg,  ImageAccessService imageAccessService,
			CustomProviderProperties source, String idFromTemplate, String orgId)
	{
		String profile = source.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE);
		if (profile != null && !profile.isEmpty())
			setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(profile));
		
		fromProperties(msg, imageAccessService, source, orgId != null ? orgId : idFromTemplate);
	}

	public void fromProperties(MessageSource msg, ImageAccessService imageAccessService,
			CustomProviderProperties source, String id)
	{
		setId(id);
		setType(source.getValue(CustomProviderProperties.PROVIDER_TYPE));
		setName(source.getLocalizedStringWithoutFallbackToDefault(msg, CustomProviderProperties.PROVIDER_NAME));
		setAuthenticationEndpoint(source.getValue(CustomProviderProperties.PROVIDER_LOCATION));
		setAccessTokenEndpoint(source.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT));
		setProfileEndpoint(source.getValue(CustomProviderProperties.PROFILE_ENDPOINT));

		if (source.isSet(CustomProviderProperties.ICON_URL))
		{
			String logoUri = source.getLocalizedString(msg, CustomProviderProperties.ICON_URL)
					.getDefaultValue();
			setLogo(imageAccessService.getEditableImageResourceFromUriWithUnknownTheme(logoUri).orElse(null));
		}

		setClientId(source.getValue(CustomProviderProperties.CLIENT_ID));
		setClientSecret(source.getValue(CustomProviderProperties.CLIENT_SECRET));
		setClientAuthenticationMode(
				source.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE, ClientAuthnMode.class));
		setClientAuthenticationModeForProfile(source.getEnumValue(
				CustomProviderProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, ClientAuthnMode.class));
		setClientHttpMethodForProfileAccess(
				source.getEnumValue(CustomProviderProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
						ClientHttpMethod.class));
		
		if (source.isSet(CustomProviderProperties.SCOPES))
		{	
			setRequestedScopes(Arrays.asList(source.getValue(CustomProviderProperties.SCOPES).split(" ")));
	
		}
		
		setAccessTokenFormat(source.getEnumValue(CustomProviderProperties.ACCESS_TOKEN_FORMAT,
				AccessTokenFormat.class));
		setOpenIdConnect(source.getBooleanValue(CustomProviderProperties.OPENID_CONNECT));
		setOpenIdDiscoverEndpoint(source.getValue(CustomProviderProperties.OPENID_DISCOVERY));
		setRegistrationForm(source.getValue(CommonWebAuthnProperties.REGISTRATION_FORM));

		if (source.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
		{
			setTranslationProfile(TranslationProfileGenerator.getProfileFromString(
					source.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE)));

		} else
		{
			setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(
					source.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE)));
		}

		if (source.isSet(CommonWebAuthnProperties.ENABLE_ASSOCIATION))
		{
			accountAssociation = source.getBooleanValue(CommonWebAuthnProperties.ENABLE_ASSOCIATION)
					? ToggleWithDefault.enable
					: ToggleWithDefault.disable;
		}

		setClientHostnameChecking(source.getEnumValue(CustomProviderProperties.CLIENT_HOSTNAME_CHECKING,
				ServerHostnameCheckingMode.class));
		setClientTrustStore(source.getValue(CustomProviderProperties.CLIENT_TRUSTSTORE));
		
		List<NameValuePair> additionalAuthzParams = source.getAdditionalAuthzParams();
		setExtraAuthorizationParameters(additionalAuthzParams.stream().map(p -> new NameValuePairBinding(p.getName(), p.getValue())).collect(Collectors.toList()));
	}

	public void toProperties(Properties raw, MessageSource msg, FileStorageService fileStorageService, String authName)
	{
		String prefix = OAuthClientProperties.P + OAuthClientProperties.PROVIDERS + id + ".";

		raw.put(prefix + CustomProviderProperties.PROVIDER_TYPE, type);
		if (getName() != null)
		{
			getName().toProperties(raw, prefix + CustomProviderProperties.PROVIDER_NAME, msg);
		}

		if (!Strings.isNullOrEmpty(authenticationEndpoint))
		{
			raw.put(prefix + CustomProviderProperties.PROVIDER_LOCATION, authenticationEndpoint);
		}

		if (!Strings.isNullOrEmpty(accessTokenEndpoint))
		{
			raw.put(prefix + CustomProviderProperties.ACCESS_TOKEN_ENDPOINT, accessTokenEndpoint);
		}

		if (!Strings.isNullOrEmpty(getProfileEndpoint()))
		{
			raw.put(prefix + CustomProviderProperties.PROFILE_ENDPOINT, getProfileEndpoint());
		}

		if (getLogo() != null)
		{
			FileFieldUtils.saveInProperties(getLogo(), prefix + CustomProviderProperties.ICON_URL, raw, fileStorageService,
					StandardOwner.AUTHENTICATOR.toString(), authName + "." + getId());
		}

		raw.put(prefix + CustomProviderProperties.CLIENT_ID, getClientId());

		raw.put(prefix + CustomProviderProperties.CLIENT_SECRET, getClientSecret());

		if (getClientAuthenticationMode() != null)
		{
			raw.put(prefix + CustomProviderProperties.CLIENT_AUTHN_MODE,
					getClientAuthenticationMode().toString());
		}

		if (getClientAuthenticationModeForProfile() != null)
		{
			raw.put(prefix + CustomProviderProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS,
					getClientAuthenticationModeForProfile().toString());
		}

		if (getClientHttpMethodForProfileAccess() != null)
		{
			raw.put(prefix + CustomProviderProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS,
					getClientHttpMethodForProfileAccess().toString());
		}

		if (getRequestedScopes() != null)
		{
			raw.put(prefix + CustomProviderProperties.SCOPES, String.join(" ", getRequestedScopes()));
		}

		if (getAccessTokenFormat() != null)
		{
			raw.put(prefix + CustomProviderProperties.ACCESS_TOKEN_FORMAT,
					getAccessTokenFormat().toString());
		}

		raw.put(prefix + CustomProviderProperties.OPENID_CONNECT, String.valueOf(isOpenIdConnect()));

		if (!Strings.isNullOrEmpty(getOpenIdDiscoverEndpoint()))
		{
			raw.put(prefix + CustomProviderProperties.OPENID_DISCOVERY, getOpenIdDiscoverEndpoint());
		}

		if (getRegistrationForm() != null)
		{
			raw.put(prefix + CommonWebAuthnProperties.REGISTRATION_FORM, getRegistrationForm());
		}

	
		if (getAccountAssociation() != ToggleWithDefault.bydefault)
		{
			raw.put(prefix + CommonWebAuthnProperties.ENABLE_ASSOCIATION,
					accountAssociation == ToggleWithDefault.enable ? "true" : "false");
		}	
		
		try
		{
			raw.put(prefix + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
					Constants.MAPPER.writeValueAsString(getTranslationProfile().toJsonObject()));
		} catch (Exception e)
		{
			throw new InternalException("Can't serialize provider's translation profile to JSON", e);
		}

		if (getClientHostnameChecking() != null)
		{
			raw.put(prefix + CustomProviderProperties.CLIENT_HOSTNAME_CHECKING,
					getClientHostnameChecking().toString());
		}

		if (getClientTrustStore() != null)
		{
			raw.put(prefix + CustomProviderProperties.CLIENT_TRUSTSTORE, getClientTrustStore());
		}

		if (getExtraAuthorizationParameters() != null)
		{
			for (NameValuePairBinding nvPair : getExtraAuthorizationParameters())
			{
				raw.put(prefix + CustomProviderProperties.ADDITIONAL_AUTHZ_PARAMS + (getExtraAuthorizationParameters().indexOf(nvPair) + 1),
						nvPair.getName() + "=" + nvPair.getValue());
			}
		}
	}

	public List<String> getRequestedScopes()
	{
		return requestedScopes;
	}

	public void setRequestedScopes(List<String> requestedScopes)
	{
		this.requestedScopes = requestedScopes;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public boolean isOpenIdConnect()
	{
		return openIdConnect;
	}

	public void setOpenIdConnect(boolean openIdConnect)
	{
		this.openIdConnect = openIdConnect;
	}

	public String getOpenIdDiscoverEndpoint()
	{
		return openIdDiscoverEndpoint;
	}

	public void setOpenIdDiscoverEndpoint(String openIdDiscoverEndpoint)
	{
		this.openIdDiscoverEndpoint = openIdDiscoverEndpoint;
	}

	public String getAuthenticationEndpoint()
	{
		return authenticationEndpoint;
	}

	public void setAuthenticationEndpoint(String authenticationEndpoint)
	{
		this.authenticationEndpoint = authenticationEndpoint;
	}

	public String getAccessTokenEndpoint()
	{
		return accessTokenEndpoint;
	}

	public void setAccessTokenEndpoint(String accessTokenEndpoint)
	{
		this.accessTokenEndpoint = accessTokenEndpoint;
	}

	public String getRegistrationForm()
	{
		return registrationForm;
	}

	public void setRegistrationForm(String registrationForm)
	{
		this.registrationForm = registrationForm;
	}

	public ToggleWithDefault getAccountAssociation()
	{
		return accountAssociation;
	}

	public void setAccountAssociation(ToggleWithDefault accountAssociation)
	{
		this.accountAssociation = accountAssociation;
	}

	public List<NameValuePairBinding> getExtraAuthorizationParameters()
	{
		return extraAuthorizationParameters;
	}

	public void setExtraAuthorizationParameters(List<NameValuePairBinding> extraAuthorizationParameters)
	{
		this.extraAuthorizationParameters = extraAuthorizationParameters;
	}

	public I18nString getName()
	{
		return name;
	}

	public void setName(I18nString name)
	{
		this.name = name;
	}

	public AccessTokenFormat getAccessTokenFormat()
	{
		return accessTokenFormat;
	}

	public void setAccessTokenFormat(AccessTokenFormat accessTokenFormat)
	{
		this.accessTokenFormat = accessTokenFormat;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public OAuthProviderConfiguration clone()
	{
		OAuthProviderConfiguration clone = new OAuthProviderConfiguration();
		clone.setId(new String(this.id));
		clone.setType(new String(this.getType()));
		clone.setName(this.getName() != null ? this.getName().clone() : null);
		clone.setAuthenticationEndpoint(
				this.getAuthenticationEndpoint() != null ? new String(this.getAuthenticationEndpoint())
						: null);
		clone.setAccessTokenEndpoint(
				this.getAccessTokenEndpoint() != null ? new String(this.getAccessTokenEndpoint())
						: null);
		clone.setProfileEndpoint(
				this.getProfileEndpoint() != null ? new String(this.getProfileEndpoint()) : null);
		clone.setLogo(this.getLogo() != null ? this.getLogo().clone() : null);
		clone.setClientId(this.getClientId() != null ? new String(this.getClientId()) : null);
		clone.setClientSecret(this.getClientSecret() != null ? new String(this.getClientSecret()) : null);
		clone.setClientAuthenticationMode(this.getClientAuthenticationMode() != null
				? ClientAuthnMode.valueOf(this.getClientAuthenticationMode().toString())
				: null);
		clone.setClientAuthenticationModeForProfile(this.getClientAuthenticationModeForProfile() != null
				? ClientAuthnMode.valueOf(this.getClientAuthenticationModeForProfile().toString())
				: null);
		clone.setClientHttpMethodForProfileAccess(this.getClientHttpMethodForProfileAccess() != null
				? ClientHttpMethod.valueOf(this.getClientHttpMethodForProfileAccess().toString())
				: null);
		clone.setRequestedScopes(this.getRequestedScopes() != null
				? this.getRequestedScopes().stream().map(s -> new String(s)).collect(Collectors.toList())
				: null);
		clone.setAccessTokenFormat(this.getAccessTokenFormat() != null
				? AccessTokenFormat.valueOf(this.getAccessTokenFormat().toString())
				: null);
		clone.setOpenIdConnect(this.isOpenIdConnect());
		clone.setOpenIdDiscoverEndpoint(
				this.getOpenIdDiscoverEndpoint() != null ? new String(this.getOpenIdDiscoverEndpoint())
						: null);
		clone.setRegistrationForm(
				this.getRegistrationForm() != null ? new String(this.getRegistrationForm()) : null);
		clone.setTranslationProfile(
				this.getTranslationProfile() != null ? this.getTranslationProfile().clone() : null);
		clone.setAccountAssociation(this.getAccountAssociation());
		clone.setClientHostnameChecking(this.getClientHostnameChecking() != null
				? ServerHostnameCheckingMode.valueOf(this.getClientHostnameChecking().toString())
				: null);
		clone.setClientTrustStore(
				this.getClientTrustStore() != null ? new String(this.getClientTrustStore()) : null);		
		clone.setExtraAuthorizationParameters(
				this.getExtraAuthorizationParameters() != null ? this.getExtraAuthorizationParameters()
						.stream().map(s -> new NameValuePairBinding(s.getName(), s.getValue()))
						.collect(Collectors.toList()) : null);
		return clone;
	}

	public LocalOrRemoteResource getLogo()
	{
		return logo;
	}

	public void setLogo(LocalOrRemoteResource logo)
	{
		this.logo = logo;
	}
}