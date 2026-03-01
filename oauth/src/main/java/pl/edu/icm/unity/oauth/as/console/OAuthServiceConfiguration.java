/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.vaadin.auth.services.idp.ActiveValueConfig;
import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfiguration;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfigurationParser;
import pl.edu.icm.unity.engine.api.idp.UserImportConfig;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;

/**
 * Represent full OAuth service configuration.
 * 
 * @author P.Piernik
 *
 */
public class OAuthServiceConfiguration
{
	private String issuerURI;
	private int idTokenExpiration;
	private int refreshTokenExpiration;
	private RefreshTokenIssuePolicy refreshTokenIssuePolicy;
	private boolean refreshTokenRotationForPublicClients;
	private int codeTokenExpiration;
	private int accessTokenExpiration;
	private boolean openIDConnect;
	private boolean skipConsentScreen;
	private String credential;
	private SigningAlgorithms signingAlg;
	private String signingSecret;
	private String identityTypeForSubject;
	private List<OAuthScopeBean> scopes;
	private TranslationProfile translationProfile;
	private GroupWithIndentIndicator clientGroup;
	private GroupWithIndentIndicator usersGroup;
	private List<ActiveValueConfig> activeValueSelections;
	private List<UserImportConfig> userImports;
	private boolean skipUserImport;
	private boolean allowForWildcardsInAllowedURI;
	private boolean allowForUnauthenticatedRevocation;
	private int maxExtendAccessTokenValidity;
	private boolean supportExtendTokenValidity;
	private AccessTokenFormat accessTokenFormat;
	private IdpPolicyAgreementsConfiguration policyAgreementConfig;
	private List<TrustedUpstreamASBean> trustedUpstreamAS;
	private List<AuthorizationScriptBean> authorizationScripts;
	private boolean tokenExchangeSupport;
	
	public OAuthServiceConfiguration()
	{

	}

	public OAuthServiceConfiguration(MessageSource msg)
	{
		policyAgreementConfig = new IdpPolicyAgreementsConfiguration(msg);
	}

	public OAuthServiceConfiguration(MessageSource msg, List<Group> allGroups, OAuthScopesService scopesService)
	{
		signingAlg = SigningAlgorithms.RS256;
		idTokenExpiration = OAuthASProperties.DEFAULT_ID_TOKEN_VALIDITY;
		codeTokenExpiration = OAuthASProperties.DEFAULT_CODE_TOKEN_VALIDITY;
		accessTokenExpiration = OAuthASProperties.DEFAULT_ACCESS_TOKEN_VALIDITY;
		refreshTokenExpiration = OAuthASProperties.DEFAULT_REFRESH_TOKEN_VALIDITY;
		setAllowForWildcardsInAllowedURI(false);
		setAllowForUnauthenticatedRevocation(false);
		setIdentityTypeForSubject(TargetedPersistentIdentity.ID);
		scopes = scopesService.getSystemScopes()
				.stream()
				.map(s -> new OAuthScopeBean(s.name, s.description))
				.collect(Collectors.toList());
		translationProfile = TranslationProfileGenerator.generateEmbeddedEmptyOutputProfile();
		Group root = allGroups.stream()
				.filter(g -> g.toString()
						.equals("/"))
				.findAny()
				.orElse(new Group("/"));
		usersGroup = new GroupWithIndentIndicator(root, false);
		clientGroup = new GroupWithIndentIndicator(root, false);
		openIDConnect = false;
		tokenExchangeSupport= false;
		supportExtendTokenValidity = false;
		skipUserImport = false;
		userImports = new ArrayList<>();
		accessTokenFormat = AccessTokenFormat.PLAIN;
		policyAgreementConfig = new IdpPolicyAgreementsConfiguration(msg);
		refreshTokenIssuePolicy = RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED;
		setRefreshTokenRotationForPublicClients(false);
		trustedUpstreamAS = new ArrayList<>();
		authorizationScripts = new ArrayList<>();
	}

	public String toProperties(MessageSource msg, PKIManagement pkiForValidation)
	{
		Properties raw = new Properties();

		raw.put(OAuthASProperties.P + OAuthASProperties.ISSUER_URI, issuerURI);
		raw.put(OAuthASProperties.P + OAuthASProperties.ID_TOKEN_VALIDITY, String.valueOf(idTokenExpiration));
		raw.put(OAuthASProperties.P + OAuthASProperties.CODE_TOKEN_VALIDITY, String.valueOf(codeTokenExpiration));
		raw.put(OAuthASProperties.P + OAuthASProperties.ACCESS_TOKEN_VALIDITY, String.valueOf(accessTokenExpiration));
		raw.put(OAuthASProperties.P + CommonIdPProperties.SKIP_CONSENT, String.valueOf(skipConsentScreen));
		raw.put(OAuthASProperties.P + OAuthASProperties.ALLOW_FOR_WILDCARDS_IN_ALLOWED_URI,
				String.valueOf(allowForWildcardsInAllowedURI));
		raw.put(OAuthASProperties.P + OAuthASProperties.ALLOW_UNAUTHENTICATED_REVOCATION,
				String.valueOf(allowForUnauthenticatedRevocation));
		raw.put(OAuthASProperties.P + OAuthASProperties.ACCESS_TOKEN_FORMAT, accessTokenFormat.toString());
		if (supportExtendTokenValidity)
		{
			raw.put(OAuthASProperties.P + OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY,
					String.valueOf(maxExtendAccessTokenValidity));
		}

		raw.put(OAuthASProperties.P + OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, refreshTokenIssuePolicy.toString());
		if (!refreshTokenIssuePolicy.equals(RefreshTokenIssuePolicy.NEVER))
		{
			raw.put(OAuthASProperties.P + OAuthASProperties.REFRESH_TOKEN_VALIDITY,
					String.valueOf(refreshTokenExpiration));
		}
		raw.put(OAuthASProperties.P + OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION,
				String.valueOf(isRefreshTokenRotationForPublicClients()));

		if (credential != null)
		{
			raw.put(OAuthASProperties.P + OAuthASProperties.CREDENTIAL, credential);
		}

		raw.put(OAuthASProperties.P + OAuthASProperties.SIGNING_ALGORITHM, String.valueOf(signingAlg));

		if (signingSecret != null)
		{
			raw.put(OAuthASProperties.P + OAuthASProperties.SIGNING_SECRET, signingSecret);
		}

		raw.put(OAuthASProperties.P + OAuthASProperties.IDENTITY_TYPE_FOR_SUBJECT, identityTypeForSubject);

		if (scopes != null)
		{
			for (OAuthScopeBean scope : scopes)
			{
				String key = OAuthASProperties.SCOPES + (scopes.indexOf(scope) + 1) + ".";
				raw.put(OAuthASProperties.P + key + OAuthASProperties.SCOPE_NAME, scope.getName());
				raw.put(OAuthASProperties.P + key + OAuthASProperties.SCOPE_ENABLED, String.valueOf(scope.isEnabled()));
				raw.put(OAuthASProperties.P + key + OAuthASProperties.SCOPE_IS_PATTERN, String.valueOf(scope.isPattern()));

				if (scope.getDescription() != null)
				{
					raw.put(OAuthASProperties.P + key + OAuthASProperties.SCOPE_DESCRIPTION, scope.getDescription());
				}

				List<String> attributes = scope.getAttributes();
				if (attributes != null)
				{
					for (String attr : attributes)
					{
						raw.put(OAuthASProperties.P + key + OAuthASProperties.SCOPE_ATTRIBUTES
								+ (attributes.indexOf(attr) + 1), attr);
					}
				}
			}
		}
		
		if (authorizationScripts != null)
		{
			for (AuthorizationScriptBean script : authorizationScripts)
			{
				String key = OAuthASProperties.AUTHORIZATION_SCRIPTS + (authorizationScripts.indexOf(script) + 1) + ".";
				raw.put(OAuthASProperties.P + key + OAuthASProperties.AUTHORIZATION_SCRIPT_TRIGGERING_SCOPE, script.getScope());
				raw.put(OAuthASProperties.P + key + OAuthASProperties.AUTHORIZATION_SCRIPT_PATH, script.getPath());				
			}
		}
		if (activeValueSelections != null)
		{
			for (ActiveValueConfig acConfig : activeValueSelections)
			{
				String key = CommonIdPProperties.ACTIVE_VALUE_SELECTION_PFX
						+ (activeValueSelections.indexOf(acConfig) + 1) + ".";
				raw.put(OAuthASProperties.P + key + CommonIdPProperties.ACTIVE_VALUE_CLIENT, acConfig.getClientId());

				List<String> sattributes = acConfig.getSingleSelectableAttributes();
				if (sattributes != null)
				{
					for (String attr : sattributes)
					{
						raw.put(OAuthASProperties.P + key + CommonIdPProperties.ACTIVE_VALUE_SINGLE_SELECTABLE
								+ (sattributes.indexOf(attr) + 1), attr);
					}
				}

				List<String> mattributes = acConfig.getMultiSelectableAttributes();
				if (mattributes != null)
				{
					for (String attr : mattributes)
					{
						raw.put(OAuthASProperties.P + key + CommonIdPProperties.ACTIVE_VALUE_MULTI_SELECTABLE
								+ (mattributes.indexOf(attr) + 1), attr);
					}
				}

			}
		}

		raw.put(OAuthASProperties.P + CommonIdPProperties.SKIP_USERIMPORT, String.valueOf(skipUserImport));

		if (userImports != null)
		{
			for (UserImportConfig impConfig : userImports)
			{
				String key = CommonIdPProperties.USERIMPORT_PFX + (userImports.indexOf(impConfig) + 1) + ".";
				raw.put(OAuthASProperties.P + key + CommonIdPProperties.USERIMPORT_IMPORTER, impConfig.importer);
				raw.put(OAuthASProperties.P + key + CommonIdPProperties.USERIMPORT_IDENTITY_TYPE,
						impConfig.type);
			}
		}

		if (trustedUpstreamAS != null)
		{
			for (TrustedUpstreamASBean trustedAS : trustedUpstreamAS)
			{
				String key = OAuthASProperties.TRUSTED_UPSTREAM_AS + (trustedUpstreamAS.indexOf(trustedAS) + 1) + ".";

				if (!Strings.isNullOrEmpty(trustedAS.getClientId()))
				{
					raw.put(OAuthASProperties.P + key + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_ID,
							trustedAS.getClientId());
				}
				if (!Strings.isNullOrEmpty(trustedAS.getClientSecret()))
				{
					raw.put(OAuthASProperties.P + key + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_SECRET,
							trustedAS.getClientSecret());
				}
				if (!Strings.isNullOrEmpty(trustedAS.getMetadataURL()))
				{
					raw.put(OAuthASProperties.P + key + OAuthASProperties.TRUSTED_UPSTREAM_AS_METADATA_URL,
							trustedAS.getMetadataURL());
				}

				raw.put(OAuthASProperties.P + key + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_HOSTNAME_CHECKING,
						trustedAS.getClientHostnameChecking()
								.name());

				if (!Strings.isNullOrEmpty(trustedAS.getClientTrustStore()))
				{
					raw.put(OAuthASProperties.P + key + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_TRUSTSTORE,
							trustedAS.getClientTrustStore());
				}

				if (!Strings.isNullOrEmpty(trustedAS.getIntrospectionEndpointURL()))
				{
					raw.put(OAuthASProperties.P + key
							+ OAuthASProperties.TRUSTED_UPSTREAM_AS_INTROSPECTION_ENDPOINT_URL,
							trustedAS.getIntrospectionEndpointURL());
				}
				if (!Strings.isNullOrEmpty(trustedAS.getIssuerURI()))
				{
					raw.put(OAuthASProperties.P + key + OAuthASProperties.TRUSTED_UPSTREAM_AS_ISSUER_URI,
							trustedAS.getIssuerURI());
				}
				if (!Strings.isNullOrEmpty(trustedAS.getCertificate()))
				{
					raw.put(OAuthASProperties.P + key + OAuthASProperties.TRUSTED_UPSTREAM_AS_CERTIFICATE,
							trustedAS.getCertificate());
				}

			}
		}

		try
		{
			raw.put(OAuthASProperties.P + CommonIdPProperties.EMBEDDED_TRANSLATION_PROFILE,
					Constants.MAPPER.writeValueAsString(getTranslationProfile().toJsonObject()));
		} catch (Exception e)
		{
			throw new InternalException("Can't serialize oauth idp translation profile to JSON", e);
		}

		raw.put(OAuthASProperties.P + OAuthASProperties.CLIENTS_GROUP, clientGroup.group().toString());
		raw.put(OAuthASProperties.P + OAuthASProperties.USERS_GROUP, usersGroup.group().toString());

		if (policyAgreementConfig != null)
		{
			raw.putAll(IdpPolicyAgreementsConfigurationParser.toProperties(msg, policyAgreementConfig,
					OAuthASProperties.P));
		}

		OAuthASProperties oauthProperties = pkiForValidation != null ? new OAuthASProperties(raw, pkiForValidation, null) : new OAuthASProperties(raw);
		return oauthProperties.getAsString();
	}

	public void fromProperties(MessageSource msg, String properties, List<Group> allGroups,
			OAuthScopesService scopeService)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth idp service", e);
		}

		OAuthASProperties oauthProperties = new OAuthASProperties(raw);
		issuerURI = oauthProperties.getIssuerName();
		idTokenExpiration = oauthProperties.getIdTokenValidity();
		refreshTokenExpiration = oauthProperties.getRefreshTokenValidity();
		refreshTokenIssuePolicy = oauthProperties.getRefreshTokenIssuePolicy();
		if (oauthProperties.isSet(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
		{
			refreshTokenRotationForPublicClients = oauthProperties
					.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION);
		}
		codeTokenExpiration = oauthProperties.getCodeTokenValidity();
		accessTokenExpiration = oauthProperties.getAccessTokenValidity();
		skipConsentScreen = oauthProperties.getBooleanValue(CommonIdPProperties.SKIP_CONSENT);
		allowForWildcardsInAllowedURI = oauthProperties
				.getBooleanValue(OAuthASProperties.ALLOW_FOR_WILDCARDS_IN_ALLOWED_URI);
		allowForUnauthenticatedRevocation = oauthProperties
				.getBooleanValue(OAuthASProperties.ALLOW_UNAUTHENTICATED_REVOCATION);
		accessTokenFormat = oauthProperties.getAccessTokenFormat();
		if (oauthProperties.isSet(OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY))
		{
			maxExtendAccessTokenValidity = oauthProperties
					.getIntValue(OAuthASProperties.MAX_EXTEND_ACCESS_TOKEN_VALIDITY);
			supportExtendTokenValidity = true;
		} else
		{
			maxExtendAccessTokenValidity = 0;
		}

		signingAlg = SigningAlgorithms.valueOf(oauthProperties.getSigningAlgorithm());
		signingSecret = oauthProperties.getValue(OAuthASProperties.SIGNING_SECRET);
		credential = oauthProperties.getValue(OAuthASProperties.CREDENTIAL);
		identityTypeForSubject = oauthProperties.getSubjectIdentityType();

		scopes.clear();
		scopeService.getScopes(oauthProperties)
				.stream()
				.forEach(s ->
				{
					OAuthScopeBean oauthScope = new OAuthScopeBean();
					oauthScope.setName(s.name);
					oauthScope.setDescription(s.description);
					oauthScope.setAttributes(s.attributes);
					oauthScope.setEnabled(s.enabled);
					oauthScope.setPattern(s.pattern);
					scopes.add(oauthScope);
				});
		
		authorizationScripts.clear();
		Set<String> scriptKeys = oauthProperties.getStructuredListKeys(OAuthASProperties.AUTHORIZATION_SCRIPTS);
		scriptKeys.forEach(scriptKey -> authorizationScripts.add(new AuthorizationScriptBean(
				oauthProperties.getValue(scriptKey + OAuthASProperties.AUTHORIZATION_SCRIPT_TRIGGERING_SCOPE),
				oauthProperties.getValue(scriptKey + OAuthASProperties.AUTHORIZATION_SCRIPT_PATH))));

		trustedUpstreamAS.clear();
		Set<String> trustedUpstreamASKeys = oauthProperties
				.getStructuredListKeys(OAuthASProperties.TRUSTED_UPSTREAM_AS);
		for (String trustedUpstreamKey : trustedUpstreamASKeys)
		{

			TrustedUpstreamASBean trustedUpstreamASBean = new TrustedUpstreamASBean();
			trustedUpstreamASBean.setClientId(
					oauthProperties.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_ID));
			trustedUpstreamASBean.setClientSecret(
					oauthProperties.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_SECRET));
			trustedUpstreamASBean.setCertificate(
					oauthProperties.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CERTIFICATE));
			trustedUpstreamASBean.setIntrospectionEndpointURL(oauthProperties
					.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_INTROSPECTION_ENDPOINT_URL));
			trustedUpstreamASBean.setIssuerURI(
					oauthProperties.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_ISSUER_URI));
			trustedUpstreamASBean.setMetadataURL(
					oauthProperties.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_METADATA_URL));
			trustedUpstreamASBean.setClientHostnameChecking(oauthProperties.getEnumValue(
					trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_HOSTNAME_CHECKING,
					ServerHostnameCheckingMode.class));
			trustedUpstreamASBean.setClientTrustStore(oauthProperties
					.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_TRUSTSTORE));
			trustedUpstreamAS.add(trustedUpstreamASBean);
		}

		openIDConnect = isScopeEnabled(OIDCScopeValue.OPENID.getValue());
		tokenExchangeSupport = isScopeEnabled(OAuthSystemScopeProvider.TOKEN_EXCHANGE_SCOPE);
		
		if (oauthProperties.isSet(CommonIdPProperties.EMBEDDED_TRANSLATION_PROFILE))
		{
			translationProfile = TranslationProfileGenerator
					.getProfileFromString(oauthProperties.getValue(CommonIdPProperties.EMBEDDED_TRANSLATION_PROFILE));

		} else if (oauthProperties.getValue(CommonIdPProperties.TRANSLATION_PROFILE) != null)
		{
			translationProfile = TranslationProfileGenerator
					.generateIncludeOutputProfile(oauthProperties.getValue(CommonIdPProperties.TRANSLATION_PROFILE));
		} else
		{
			translationProfile = TranslationProfileGenerator
					.generateIncludeOutputProfile(TranslationProfileManagement.DEFAULT_OUTPUT_PROFILE);
		}

		String clientGroupPath = oauthProperties.getValue(OAuthASProperties.CLIENTS_GROUP);
		clientGroup =

				new GroupWithIndentIndicator(allGroups.stream()
						.filter(g -> g.toString()
								.equals(clientGroupPath))
						.findFirst()
						.orElse(new Group(clientGroupPath)), false);

		String usersGroupPath = oauthProperties.getValue(OAuthASProperties.USERS_GROUP);
		usersGroup =

				new GroupWithIndentIndicator(allGroups.stream()
						.filter(g -> g.toString()
								.equals(usersGroupPath))
						.findFirst()
						.orElse(new Group(usersGroupPath)), false);

		activeValueSelections = new ArrayList<>();

		Set<String> attrKeys = oauthProperties.getStructuredListKeys(CommonIdPProperties.ACTIVE_VALUE_SELECTION_PFX);

		for (String attrKey : attrKeys)
		{
			String id = oauthProperties.getValue(attrKey + CommonIdPProperties.ACTIVE_VALUE_CLIENT);
			List<String> sattrs = oauthProperties
					.getListOfValues(attrKey + CommonIdPProperties.ACTIVE_VALUE_SINGLE_SELECTABLE);
			List<String> mattrs = oauthProperties
					.getListOfValues(attrKey + CommonIdPProperties.ACTIVE_VALUE_MULTI_SELECTABLE);
			ActiveValueConfig ativeValConfig = new ActiveValueConfig();
			ativeValConfig.setClientId(id);
			ativeValConfig.setSingleSelectableAttributes(sattrs);
			ativeValConfig.setMultiSelectableAttributes(mattrs);
			activeValueSelections.add(ativeValConfig);
		}

		skipUserImport = oauthProperties.getBooleanValue(CommonIdPProperties.SKIP_USERIMPORT);

		Set<String> importKeys = oauthProperties.getStructuredListKeys(CommonIdPProperties.USERIMPORT_PFX);
		for (String importKey : importKeys)
		{
			String importer = oauthProperties.getValue(importKey + CommonIdPProperties.USERIMPORT_IMPORTER);
			String identityType = oauthProperties.getValue(importKey + CommonIdPProperties.USERIMPORT_IDENTITY_TYPE);

			UserImportConfig userImportConfig = new UserImportConfig(null, importer, identityType);
			userImports.add(userImportConfig);
		}

		policyAgreementConfig = IdpPolicyAgreementsConfigurationParser.fromPropoerties(msg, oauthProperties);
	}
	
	private boolean isScopeEnabled(String scopeName)
	{
		return scopes.stream()
				.filter(s -> scopeName.equals(s.getName()))
				.findFirst()
				.map(OAuthScopeBean::isEnabled)
				.orElse(false);
	}

	public List<UserImportConfig> getUserImports()
	{
		return userImports;
	}

	public void setUserImports(List<UserImportConfig> userImports)
	{
		this.userImports = userImports;
	}

	public void setMaxExtendAccessTokenValidity(int maxExtendAccessTokenValidity)
	{
		this.maxExtendAccessTokenValidity = maxExtendAccessTokenValidity;
	}

	public String getIssuerURI()
	{
		return issuerURI;
	}

	public void setIssuerURI(String issuerURI)
	{
		this.issuerURI = issuerURI;
	}

	public int getIdTokenExpiration()
	{
		return idTokenExpiration;
	}

	public void setIdTokenExpiration(int tokenExpiration)
	{
		this.idTokenExpiration = tokenExpiration;
	}

	public int getRefreshTokenExpiration()
	{
		return refreshTokenExpiration;
	}

	public void setRefreshTokenExpiration(int refreshTokenExpiration)
	{
		this.refreshTokenExpiration = refreshTokenExpiration;
	}

	public int getCodeTokenExpiration()
	{
		return codeTokenExpiration;
	}

	public void setCodeTokenExpiration(int codeTokenExpiration)
	{
		this.codeTokenExpiration = codeTokenExpiration;
	}

	public boolean isOpenIDConnect()
	{
		return openIDConnect;
	}

	public void setOpenIDConnect(boolean openIDConnect)
	{
		this.openIDConnect = openIDConnect;
	}

	public String getCredential()
	{
		return credential;
	}

	public void setCredential(String credential)
	{
		this.credential = credential;
	}

	public SigningAlgorithms getSigningAlg()
	{
		return signingAlg;
	}

	public void setSigningAlg(SigningAlgorithms signingAlg)
	{
		this.signingAlg = signingAlg;
	}

	public int getAccessTokenExpiration()
	{
		return accessTokenExpiration;
	}

	public void setAccessTokenExpiration(int accessTokenExpiration)
	{
		this.accessTokenExpiration = accessTokenExpiration;
	}

	public String getSigningSecret()
	{
		return signingSecret;
	}

	public void setSigningSecret(String signingSecret)
	{
		this.signingSecret = signingSecret;
	}

	public String getIdentityTypeForSubject()
	{
		return identityTypeForSubject;
	}

	public void setIdentityTypeForSubject(String identityTypeForSubject)
	{
		this.identityTypeForSubject = identityTypeForSubject;
	}

	public List<OAuthScopeBean> getScopes()
	{
		return scopes;
	}

	public void setScopes(List<OAuthScopeBean> scopes)
	{
		this.scopes = scopes;
	}

	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(TranslationProfile translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public GroupWithIndentIndicator getClientGroup()
	{
		return clientGroup;
	}

	public void setClientGroup(GroupWithIndentIndicator clientGroup)
	{
		this.clientGroup = clientGroup;
	}

	public GroupWithIndentIndicator getUsersGroup()
	{
		return usersGroup;
	}

	public void setUsersGroup(GroupWithIndentIndicator usersGroup)
	{
		this.usersGroup = usersGroup;
	}

	public List<ActiveValueConfig> getActiveValueSelections()
	{
		return activeValueSelections;
	}

	public void setActiveValueSelections(List<ActiveValueConfig> activeValueSelections)
	{
		this.activeValueSelections = activeValueSelections;
	}

	public boolean isSkipConsentScreen()
	{
		return skipConsentScreen;
	}

	public void setSkipConsentScreen(boolean skipConsentScreen)
	{
		this.skipConsentScreen = skipConsentScreen;
	}

	public boolean isSkipUserImport()
	{
		return skipUserImport;
	}

	public void setSkipUserImport(boolean skipUserImport)
	{
		this.skipUserImport = skipUserImport;
	}

	public boolean isAllowForWildcardsInAllowedURI()
	{
		return allowForWildcardsInAllowedURI;
	}

	public void setAllowForWildcardsInAllowedURI(boolean allowForWildcardsInAllowedURI)
	{
		this.allowForWildcardsInAllowedURI = allowForWildcardsInAllowedURI;
	}

	public boolean isAllowForUnauthenticatedRevocation()
	{
		return allowForUnauthenticatedRevocation;
	}

	public void setAllowForUnauthenticatedRevocation(boolean allowForUnauthenticatedRevocation)
	{
		this.allowForUnauthenticatedRevocation = allowForUnauthenticatedRevocation;
	}

	public int getMaxExtendAccessTokenValidity()
	{
		return maxExtendAccessTokenValidity;
	}

	public void setMaxExtendAccessTokenValidity(Integer maxExtendAccessTokenValidity)
	{
		this.maxExtendAccessTokenValidity = maxExtendAccessTokenValidity;
	}

	public boolean isSupportExtendTokenValidity()
	{
		return supportExtendTokenValidity;
	}

	public void setSupportExtendTokenValidity(boolean supportExtendTokenValidity)
	{
		this.supportExtendTokenValidity = supportExtendTokenValidity;
	}

	public AccessTokenFormat getAccessTokenFormat()
	{
		return accessTokenFormat;
	}

	public void setAccessTokenFormat(AccessTokenFormat accessTokenFormat)
	{
		this.accessTokenFormat = accessTokenFormat;
	}

	public IdpPolicyAgreementsConfiguration getPolicyAgreementConfig()
	{
		return policyAgreementConfig;
	}

	public void setPolicyAgreementConfig(IdpPolicyAgreementsConfiguration policyAgreementConfig)
	{
		this.policyAgreementConfig = policyAgreementConfig;
	}

	public RefreshTokenIssuePolicy getRefreshTokenIssuePolicy()
	{
		return refreshTokenIssuePolicy;
	}

	public void setRefreshTokenIssuePolicy(RefreshTokenIssuePolicy refreshTokenIssuePolicy)
	{
		this.refreshTokenIssuePolicy = refreshTokenIssuePolicy;
	}

	public boolean isRefreshTokenRotationForPublicClients()
	{
		return refreshTokenRotationForPublicClients;
	}

	public void setRefreshTokenRotationForPublicClients(boolean refreshTokenRotationForPublicClients)
	{
		this.refreshTokenRotationForPublicClients = refreshTokenRotationForPublicClients;
	}

	public List<TrustedUpstreamASBean> getTrustedUpstreamAS()
	{
		return trustedUpstreamAS;
	}

	public void setTrustedUpstreamAS(List<TrustedUpstreamASBean> trustedUpstreamAS)
	{
		this.trustedUpstreamAS = trustedUpstreamAS;
	}

	public List<AuthorizationScriptBean> getAuthorizationScripts()
	{
		return authorizationScripts;
	}

	public void setAuthorizationScripts(List<AuthorizationScriptBean> authorizationScripts)
	{
		this.authorizationScripts = authorizationScripts;
	}

	public boolean isTokenExchangeSupport()
	{
		return tokenExchangeSupport;
	}

	public void setTokenExchangeSupport(boolean exchangeToken)
	{
		this.tokenExchangeSupport = exchangeToken;
	}
}