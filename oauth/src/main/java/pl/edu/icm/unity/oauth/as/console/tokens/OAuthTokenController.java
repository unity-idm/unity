/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Component
class OAuthTokenController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, OAuthTokenController.class);

	private SecuredTokensManagement tokenMan;
	private EndpointManagement endpointMan;
	private MessageSource msg;

	private final OAuthAccessTokenRepository accessTokenRepository;
	private final OAuthRefreshTokenRepository refreshTokenRepository;
	private final BulkGroupQueryService bulkService;
	private final AttributeSupport attributeSupport;

	@Autowired
	OAuthTokenController(MessageSource msg, OAuthAccessTokenRepository oauthTokenRepository,
			OAuthRefreshTokenRepository refreshTokenRepository,
			SecuredTokensManagement tokensManagement,
			@Qualifier("insecure") BulkGroupQueryService bulkService,
			AttributeSupport attributeSupport,
			EndpointManagement endpointMan)
	{
		this.msg = msg;
		this.accessTokenRepository = oauthTokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenMan = tokensManagement;
		this.bulkService = bulkService;
		this.attributeSupport = attributeSupport;
		this.endpointMan = endpointMan;
	}

	private Optional<Endpoint> getEndpoint(String name)
	{
		try
		{
			return endpointMan.getEndpoints().stream()
					.filter(e -> e.getTypeId().equals(OAuthAuthzWebEndpoint.Factory.TYPE.getName())
							&& e.getName().equals(name))
					.findFirst();
		} catch (EngineException e)
		{
			return Optional.empty();
		}

	}

	private String getIssuerUri(Endpoint endpoint)
	{

		try
		{
			Properties raw = new Properties();
			raw.load(new StringReader(endpoint.getConfiguration().getConfiguration()));
			OAuthASProperties oauthProperties = new OAuthASProperties(raw);
			return oauthProperties.getIssuerName();
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth idp service", e);
		}
	}

	public Collection<OAuthTokenBean> getOAuthTokens(String serviceName) throws ControllerException
	{
		log.trace("getOAuthTokens start [service={}]", serviceName);

		Optional<Endpoint> endpoint = getEndpoint(serviceName);
		if (!endpoint.isPresent())
		{
			log.trace("getOAuthTokens: endpoint not found [service={}]", serviceName);
			return Collections.emptyList();
		}

		try
		{
			String issuerUri = getIssuerUri(endpoint.get());
			record TokenPair(Token raw, OAuthToken oauth) {}

			List<Token> allTokens = getTokens();
			log.trace("getOAuthTokens: loaded all tokens [count={}]", allTokens.size());

			List<TokenPair> filtered = allTokens.stream()
					.map(t -> new TokenPair(t, OAuthToken.getInstanceFromJson(t.getContents())))
					.filter(p -> issuerUri.equals(p.oauth().getIssuerUri()))
					.collect(Collectors.toList());
			log.trace("getOAuthTokens: filtered by issuerUri [matched={}, issuerUri={}]",
					filtered.size(), issuerUri);

			Map<Long, String> ownerLabels = buildOwnerLabels(
					filtered.stream().map(TokenPair::raw).collect(Collectors.toList()));
			log.trace("getOAuthTokens: built owner labels [owners={}]", ownerLabels.size());

			Collection<OAuthTokenBean> result = filtered.stream()
					.map(p -> new OAuthTokenBean(p.raw(), p.oauth(), msg,
							ownerLabels.getOrDefault(p.raw().getOwner(), "[" + p.raw().getOwner() + "]")))
					.collect(Collectors.toList());

			log.trace("getOAuthTokens done [service={}, result={}]", serviceName, result.size());
			return result;
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("OAuthTokenController.getTokensError", serviceName), e);
		}
	}

	private Map<Long, String> buildOwnerLabels(List<Token> tokens) throws EngineException
	{
		Set<Long> ownerIds = tokens.stream()
				.map(Token::getOwner)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (ownerIds.isEmpty())
			return Collections.emptyMap();

		log.trace("buildOwnerLabels: fetching membership data [owners={}]", ownerIds.size());
		String nameAttr = getEntityNameAttribute();
		GroupMembershipData membershipData = bulkService.getBulkMembershipData("/", ownerIds);
		log.trace("buildOwnerLabels: got bulk membership data, resolving entity info");
		Map<Long, EntityInGroupData> entitiesData = bulkService.getMembershipInfo(membershipData);
		log.trace("buildOwnerLabels: resolved entity info [entities={}]", entitiesData.size());

		Map<Long, String> labels = new HashMap<>();
		for (Long ownerId : ownerIds)
		{
			String idLabel = "[" + ownerId + "]";
			EntityInGroupData data = entitiesData.get(ownerId);
			String name = null;
			if (data != null && nameAttr != null && data.rootAttributesByName.containsKey(nameAttr))
			{
				List<?> values = data.rootAttributesByName.get(nameAttr).getValues();
				if (!values.isEmpty())
					name = values.get(0).toString();
			}
			labels.put(ownerId, name != null ? idLabel + " " + name : idLabel);
		}
		return labels;
	}

	private String getEntityNameAttribute()
	{
		try
		{
			List<AttributeType> nameAttrs = attributeSupport
					.getAttributeTypeWithMetadata(EntityNameMetadataProvider.NAME);
			return nameAttrs.isEmpty() ? null : nameAttrs.get(0).getName();
		} catch (Exception e)
		{
			log.debug("Can't determine entity name attribute type", e);
			return null;
		}
	}

	private List<Token> getTokens() throws EngineException
	{
		List<Token> tokens = new ArrayList<>();
		tokens.addAll(accessTokenRepository.getAllAccessTokens());
		tokens.addAll(refreshTokenRepository.getAllRefreshTokens());
		return tokens;
	}

	public void removeToken(OAuthTokenBean item) throws ControllerException
	{
		try
		{
			tokenMan.removeToken(item.getRealType(), item.getId());
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("OAuthTokenController.removeTokenError", item.getId()), e);
		}

	}

}