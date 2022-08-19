/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.idp.TechnicalInformationProperty;
import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.IdPClientData;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.idp.TrustedIdPClientsManagement;
import io.imunity.idp.IdPClientData.AccessStatus;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement.LastIdPClientAccessKey;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.oauth.as.token.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.webui.console.services.idp.IdpUsersHelper;
import pl.edu.icm.unity.webui.idpcommon.URIPresentationHelper;

@Component
public class TrustedOAuthClientsManagement implements TrustedIdPClientsManagement
{
	private final SecuredTokensManagement tokenMan;
	private final PreferencesManagement preferencesManagement;
	private final OAuthAccessTokenRepository accessTokenDAO;
	private final OAuthRefreshTokenRepository refreshTokenDAO;
	private final EndpointManagement endpointManagement;
	private final AttributeTypeSupport aTypeSupport;

	private final BulkGroupQueryService bulkService;
	private final IdpUsersHelper idpUsersHelper;
	private final MessageSource msg;
	private final OAuthScopesService scopesService;
	private final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;

	public TrustedOAuthClientsManagement(SecuredTokensManagement tokenMan, PreferencesManagement preferencesManagement,
			OAuthAccessTokenRepository accessTokenDAO, OAuthRefreshTokenRepository refreshTokenDAO, @Qualifier("insecure") EndpointManagement endpointManagement,
			@Qualifier("insecure") BulkGroupQueryService bulkService, IdpUsersHelper idpUsersHelper, MessageSource msg,
			OAuthScopesService scopesService, AttributeTypeSupport aTypeSupport,
			LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement)
	{
		this.tokenMan = tokenMan;
		this.preferencesManagement = preferencesManagement;
		this.accessTokenDAO = accessTokenDAO;
		this.refreshTokenDAO = refreshTokenDAO;
		this.endpointManagement = endpointManagement;
		this.bulkService = bulkService;
		this.idpUsersHelper = idpUsersHelper;
		this.msg = msg;
		this.scopesService = scopesService;
		this.aTypeSupport = aTypeSupport;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
	}

	@Override
	public List<IdPClientData> getIdpClientsData() throws EngineException
	{
		Map<String, TokensAndPreferences> perClientData = getGroupedByClientPreferencesAndTokens();
		List<OAuthServiceConfiguration> services = getServices();
		List<IdPClientData> ret = new ArrayList<>();
		for (OAuthServiceConfiguration service : services)
		{
			for (String client : perClientData.keySet())
			{
				List<OAuthTokenWithTime> accessTokens = perClientData.get(client).tokens.stream()
						.filter(t -> t.type.equals(OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN)
								&& t.token.getIssuerUri().equals(service.issuerURI))
						.sorted((t1, t2) -> t2.createdTime.compareTo(t1.createdTime)).collect(Collectors.toList());
				List<OAuthTokenWithTime> refreshTokens = perClientData.get(client).tokens.stream()
						.filter(t -> t.type.equals(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN)
								&& t.token.getIssuerUri().equals(service.issuerURI))
						.sorted((t1, t2) -> t2.createdTime.compareTo(t1.createdTime)).collect(Collectors.toList());

				Optional<OAuthClientInfo> serviceClient = service.clients.stream().filter(c -> c.id.equals(client))
						.findAny();

				if (serviceClient.isPresent())
				{

					if (isDisallowed(perClientData.get(client).preferences))
					{
						ret.add(IdPClientData.builder().withApplicationId(new ApplicationId(client))
								.withLogo(Optional.ofNullable(serviceClient.get().logo))
								.withAccessProtocol(AccessProtocol.OAuth)
								.withAccessStatus(AccessStatus.disallowWithoutAsking)
								.withApplicationName(serviceClient.get().name)
								.withApplicationDomain(Optional.of(URIPresentationHelper
										.getHumanReadableDomain(serviceClient.get().redirectURIs.get(0))))
								.build());
					}

					if (accessTokens.size() > 0 || refreshTokens.size() > 0
							|| isAllowedWithoutAsking(perClientData.get(client).preferences))
					{
						Set<String> scopes = getScopes(accessTokens, service);

						ret.add(IdPClientData.builder().withApplicationId(new ApplicationId(client))
								.withLogo(Optional.ofNullable(serviceClient.get().logo))
								.withAccessProtocol(AccessProtocol.OAuth)
								.withLastAccessTime(Optional.ofNullable(getLastAccessByClient()
										.get(new LastIdPClientAccessKey(AccessProtocol.OAuth, client))))
								.withAccessStatus(isAllowedWithoutAsking(perClientData.get(client).preferences)
										? AccessStatus.allowWithoutAsking
										: AccessStatus.allow)
								.withAccessGrantTime(getGrantTime(refreshTokens, perClientData.get(client).preferences))
								.withApplicationName(
										serviceClient.get().title == null || serviceClient.get().title.isEmpty()
												? serviceClient.get().name
												: serviceClient.get().title)
								.withAccessScopes(Optional.ofNullable(
										scopes.size() > 0 ? scopes.stream().collect(Collectors.toList()) : null))
								.withApplicationDomain(Optional.of(URIPresentationHelper
										.getHumanReadableDomain(serviceClient.get().redirectURIs.get(0))))
								.withTechnicalInformations(getTechnicalInformations(accessTokens, refreshTokens))
								.build());
					}
				}
			}
		}

		return ret;
	}

	private Set<String> getScopes(List<OAuthTokenWithTime> accessTokens, OAuthServiceConfiguration service)
	{
		Set<String> scopes = new HashSet<>();
		for (int i = 0; i < accessTokens.size(); i++)
		{
			scopes.addAll(getScopes(accessTokens.get(i), service));
		}

		return scopes;
	}

	private List<TechnicalInformationProperty> getTechnicalInformations(List<OAuthTokenWithTime> accessTokens,
			List<OAuthTokenWithTime> refreshTokens)
	{
		List<TechnicalInformationProperty> technicalInformations = new ArrayList<>();
		for (int i = 0; i < accessTokens.size(); i++)
		{
			technicalInformations.add(TechnicalInformationProperty.builder()
					.withTitleKey(msg.getMessage("OAuthApplicationProvider.accessTokenLabel")
							+ (accessTokens.size() > 1 ? " (" + (i + 1) + "):" : ":"))
					.withValue(getTokenRepresentation(accessTokens.get(i).createdTime, accessTokens.get(i).expiredTime,
							accessTokens.get(i).token.getAccessToken()))
					.build());
		}

		for (int i = 0; i < refreshTokens.size(); i++)
		{
			technicalInformations.add(TechnicalInformationProperty.builder()
					.withTitleKey(msg.getMessage("OAuthApplicationProvider.refreshTokenLabel")
							+ (refreshTokens.size() > 1 ? " (" + (i + 1) + "):" : ":"))
					.withValue(getTokenRepresentation(refreshTokens.get(i).createdTime,
							refreshTokens.get(i).expiredTime, refreshTokens.get(i).token.getRefreshToken()))
					.build());
		}

		return technicalInformations;
	}

	private String getTokenRepresentation(Instant createTime, Instant expired, String value)
	{
		return value + "<br>"
				+ ((createTime != null
						? (msg.getMessage("OAuthApplicationProvider.issuedOn") + " "
								+ TimeUtil.formatStandardInstant(createTime))
						: "")
						+ (expired != null
								? "<br>" + msg.getMessage("OAuthApplicationProvider.expiresOn") + " "
										+ TimeUtil.formatStandardInstant(expired)
								: ""));

	}

	private Map<LastIdPClientAccessKey, Instant> getLastAccessByClient() throws EngineException
	{
		return lastAccessAttributeManagement.getLastAccessByClient();

	}

	private Optional<Instant> getGrantTime(List<OAuthTokenWithTime> refreshTokens,
			Optional<OAuthClientSettings> preferences)
	{
		if (refreshTokens.isEmpty() && (preferences.isEmpty() || preferences.get().getTimestamp() == null))
		{
			return Optional.empty();
		}
		if (preferences.isEmpty())
			return Optional.of(refreshTokens.get(refreshTokens.size() - 1).createdTime);

		if (refreshTokens.isEmpty())
		{
			return Optional.of(preferences.get().getTimestamp());
		}

		return Optional.of(
				refreshTokens.get(refreshTokens.size() - 1).createdTime.compareTo(preferences.get().getTimestamp()) < 0
						? refreshTokens.get(refreshTokens.size() - 1).createdTime
						: preferences.get().getTimestamp());
	}

	private List<String> getScopes(OAuthTokenWithTime accessToken, OAuthServiceConfiguration service)
	{
		List<String> scopes = new ArrayList<>();
		for (String scope : accessToken.token.getEffectiveScope())
		{
			Optional<OAuthScope> desc = service.scopes.stream().filter(s -> s.name.equals(scope)).findFirst();
			if (desc.isEmpty() || desc.get().description == null)
			{
				scopes.add(scope);
			} else
			{
				scopes.add(desc.get().description);
			}

		}
		return scopes;
	}

	private Map<String, TokensAndPreferences> getGroupedByClientPreferencesAndTokens() throws EngineException
	{
		Map<String, TokensAndPreferences> perClientData = new HashMap<>();

		OAuthPreferences preferences = getPreferences();
		List<OAuthTokenWithTime> tokens = getTokens();

		for (String clientPref : preferences.getKeys())
		{
			if (perClientData.containsKey(clientPref))
				perClientData.get(clientPref).setPreferences(preferences.getSPSettings(clientPref));
			else
				perClientData.put(clientPref, new TokensAndPreferences(preferences.getSPSettings(clientPref)));
		}

		for (OAuthTokenWithTime token : tokens)
		{
			if (perClientData.containsKey(token.token.getClientUsername()))
				perClientData.get(token.token.getClientUsername()).getTokens().add(token);
			else
				perClientData.put(token.token.getClientUsername(), new TokensAndPreferences(token));
		}
		return perClientData;
	}

	private boolean isDisallowed(Optional<OAuthClientSettings> preferences)
	{
		return preferences.isPresent() && !preferences.get().isDefaultAccept() && preferences.get().isDoNotAsk();
	}

	private boolean isAllowedWithoutAsking(Optional<OAuthClientSettings> preferences)
	{
		return preferences.isPresent() && preferences.get().isDefaultAccept() && preferences.get().isDoNotAsk();
	}

	protected List<OAuthTokenWithTime> getTokens() throws EngineException
	{
		List<OAuthTokenWithTime> tokens = new ArrayList<>();
		tokens.addAll(accessTokenDAO.getOwnedAccessTokens().stream()
				.map(t -> new OAuthTokenWithTime(t.getType(), t.getCreated().toInstant(),
						t.getExpires() != null ? t.getExpires().toInstant() : null,
						OAuthToken.getInstanceFromJson(t.getContents()), t.getValue()))
				.collect(Collectors.toList()));
		tokens.addAll(refreshTokenDAO.getOwnedRefreshTokens().stream()
				.map(t -> new OAuthTokenWithTime(t.getType(), t.getCreated().toInstant(),
						t.getExpires() != null ? t.getExpires().toInstant() : null,
						OAuthToken.getInstanceFromJson(t.getContents()), t.getValue()))
				.collect(Collectors.toList()));

		return tokens;
	}

	private OAuthPreferences getPreferences() throws EngineException
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		EntityParam entityParam = new EntityParam(entity.getEntityId());
		String pref = preferencesManagement.getPreference(entityParam, OAuthPreferences.ID);
		OAuthPreferences preferences = new OAuthPreferences();
		if (pref != null)
			preferences.setSerializedConfiguration(JsonUtil.parse(pref));
		return preferences;
	}

	private List<OAuthServiceConfiguration> getServices() throws AuthorizationException
	{
		List<OAuthServiceConfiguration> ret = new ArrayList<>();
		for (Endpoint endpoint : endpointManagement.getEndpoints().stream()
				.filter(e -> e.getTypeId().equals(OAuthAuthzWebEndpoint.Factory.TYPE.getName()))
				.collect(Collectors.toList()))
		{
			OAuthServiceConfiguration config = new OAuthServiceConfiguration(msg,
					endpoint.getConfiguration().getConfiguration(), scopesService);
			ret.add(config);
		}

		return ret;
	}

	private void clearPreferences(String appId) throws EngineException
	{
		OAuthPreferences preferences = getPreferences();
		preferences.removeSPSettings(appId);
		OAuthPreferences.savePreferences(preferencesManagement, preferences);
	}

	@Override
	public synchronized void unblockAccess(ApplicationId appId) throws EngineException
	{
		clearPreferences(appId.id);
	}

	@Override
	public synchronized void revokeAccess(ApplicationId appId) throws EngineException
	{
		List<OAuthTokenWithTime> tokens = getTokens();
		for (OAuthTokenWithTime token : tokens)
		{
			if (token.token.getClientUsername().equals(appId.id))
			{
				tokenMan.removeToken(token.type, token.value);
			}
		}

		clearPreferences(appId.id);
	}

	@Override
	public AccessProtocol getSupportedProtocol()
	{
		return AccessProtocol.OAuth;
	}

	private static class OAuthTokenWithTime
	{
		public final OAuthToken token;
		public final Instant createdTime;
		public final Instant expiredTime;
		public final String type;
		public final String value;

		public OAuthTokenWithTime(String type, Instant createdTime, Instant expiredTime, OAuthToken token, String value)
		{
			this.type = type;
			this.token = token;
			this.createdTime = createdTime;
			this.expiredTime = expiredTime;
			this.value = value;
		}
	}

	private static class OAuthClientInfo
	{
		public final String name;
		public final String id;
		public final List<String> redirectURIs;
		public final byte[] logo;
		public final String title;

		private OAuthClientInfo(Builder builder)
		{

			this.name = builder.name;
			this.id = builder.id;
			this.redirectURIs = builder.redirectURIs;
			this.logo = builder.logo;
			this.title = builder.title;
		}

		public static Builder builder()
		{
			return new Builder();
		}

		public static final class Builder
		{

			private String name;
			private String id;
			private List<String> redirectURIs = Collections.emptyList();
			private byte[] logo;
			private String title;

			private Builder()
			{
			}

			public Builder withName(String name)
			{
				this.name = name;
				return this;
			}

			public Builder withId(String id)
			{
				this.id = id;
				return this;
			}

			public Builder withRedirectURIs(List<String> redirectURIs)
			{
				this.redirectURIs = redirectURIs;
				return this;
			}

			public Builder withLogo(byte[] logo)
			{
				this.logo = logo;
				return this;
			}

			public Builder withTitle(String title)
			{
				this.title = title;
				return this;
			}

			public OAuthClientInfo build()
			{
				return new OAuthClientInfo(this);
			}
		}
	}

	private static class TokensAndPreferences
	{
		private List<OAuthTokenWithTime> tokens;
		private Optional<OAuthClientSettings> preferences;

		public TokensAndPreferences(OAuthClientSettings clientPref)
		{
			this.preferences = Optional.of(clientPref);
			this.tokens = new ArrayList<>();
		}

		public TokensAndPreferences(OAuthTokenWithTime token)
		{
			this.tokens = new ArrayList<>();
			this.preferences = Optional.empty();
			tokens.add(token);
		}

		public List<OAuthTokenWithTime> getTokens()
		{
			return tokens;
		}

		public void setPreferences(OAuthClientSettings preferences)
		{
			this.preferences = Optional.ofNullable(preferences);
		}
	}

	private class OAuthServiceConfiguration
	{
		public final String issuerURI;
		public final List<OAuthScope> scopes;
		public final List<OAuthClientInfo> clients;

		public OAuthServiceConfiguration(MessageSource msg, String properties, OAuthScopesService scopeService)
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

			scopes = new ArrayList<>();
			scopeService.getScopes(oauthProperties).stream().forEach(s ->
			{
				scopes.add(s);
			});

			String clientGroupPath = oauthProperties.getValue(OAuthASProperties.CLIENTS_GROUP);
			clients = getOAuthClients(clientGroupPath);
		}

		private List<OAuthClientInfo> getOAuthClients(String group)
		{
			try
			{
				List<OAuthClientInfo> clients = new ArrayList<>();
				Map<Long, EntityInGroupData> membershipInfo = bulkService
						.getMembershipInfo(bulkService.getBulkMembershipData(group));
				String nameAttr = idpUsersHelper.getClientNameAttr();

				for (EntityInGroupData member : membershipInfo.values())
				{
					if (isOAuthClient(member))
						clients.add(getOAuthClient(member, group, nameAttr));
				}
				return clients;
			} catch (EngineException e)
			{
				throw new RuntimeEngineException(e);
			}
		}

		private boolean isOAuthClient(EntityInGroupData candidate)
		{
			return candidate.groupAttributesByName.keySet().contains(OAuthSystemAttributesProvider.ALLOWED_FLOWS)
					&& getUserName(candidate.entity.getIdentities()) != null;
		}

		private OAuthClientInfo getOAuthClient(EntityInGroupData info, String group, String nameAttr)
				throws EngineException
		{
			OAuthClientInfo.Builder c = OAuthClientInfo.builder();
			c.withId(getUserName(info.entity.getIdentities()));

			Map<String, AttributeExt> attrs = info.groupAttributesByName;

			if (attrs.containsKey(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI))
			{
				c.withRedirectURIs(attrs.get(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI).getValues());
			}

			if (attrs.containsKey(OAuthSystemAttributesProvider.CLIENT_NAME))
			{
				c.withTitle(attrs.get(OAuthSystemAttributesProvider.CLIENT_NAME).getValues().get(0));
			}

			if (nameAttr != null && info.rootAttributesByName.containsKey(nameAttr))
			{
				c.withName(info.rootAttributesByName.get(nameAttr).getValues().get(0));
			}

			if (attrs.containsKey(OAuthSystemAttributesProvider.CLIENT_LOGO))
			{
				Attribute logo = attrs.get(OAuthSystemAttributesProvider.CLIENT_LOGO);
				ImageAttributeSyntax syntax = (ImageAttributeSyntax) aTypeSupport.getSyntax(logo);
				UnityImage image = syntax.convertFromString(logo.getValues().get(0));
				c.withLogo(image.getImage());
			}
			return c.build();
		}

		private String getUserName(List<Identity> identities)
		{
			for (Identity i : identities)
				if (i.getTypeId().equals(UsernameIdentity.ID))
					return i.getValue();
			return null;
		}
	}
}
