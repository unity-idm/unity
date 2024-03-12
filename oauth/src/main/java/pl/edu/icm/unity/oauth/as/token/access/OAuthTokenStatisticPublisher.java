/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticEvent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public class OAuthTokenStatisticPublisher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthTokenStatisticPublisher.class);

	private final ApplicationEventPublisher eventPublisher;
	private final MessageSource msg;
	private final EntityManagement idMan;
	private final ResolvedEndpoint endpoint;
	private final EndpointManagement endpointMan;
	private final LastIdPClinetAccessAttributeManagement lastIdPClinetAccessAttributeManagement;
	private final AttributesManagement unsecureAttributesMan;
	private final OAuthASProperties oauthConfig;

	private Endpoint authzEndpoint;

	OAuthTokenStatisticPublisher(ApplicationEventPublisher eventPublisher, MessageSource msg, EntityManagement idMan,
			OAuthRequestValidator requestValidator, EndpointManagement endpointMan,
			LastIdPClinetAccessAttributeManagement lastIdPClinetAccessAttributeManagement,
			AttributesManagement unsecureAttributesMan, OAuthASProperties oauthConfig, ResolvedEndpoint endpoint)
	{
		this.eventPublisher = eventPublisher;
		this.msg = msg;
		this.idMan = idMan;
		this.endpoint = endpoint;
		this.endpointMan = endpointMan;
		this.lastIdPClinetAccessAttributeManagement = lastIdPClinetAccessAttributeManagement;
		this.unsecureAttributesMan = unsecureAttributesMan;
		this.oauthConfig = oauthConfig;

	}

	void reportFailAsLoggedClient()
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		if (loginSession == null)
		{
			log.debug("Can not retrieve identity of the OAuth client, skippig error reporting");
			return;
		}

		EntityParam clientEntity = new EntityParam(loginSession.getEntityId());
		Entity clientResolvedEntity;
		try
		{
			clientResolvedEntity = idMan.getEntity(clientEntity);

		} catch (Exception e)
		{
			log.debug("Can not retrieving identity of the OAuth client", e);
			return;
		}

		Identity username = clientResolvedEntity.getIdentities().stream()
				.filter(i -> i.getTypeId().equals(UsernameIdentity.ID)).findFirst().orElse(null);
		String clientName;
		try
		{
			clientName = getClientName(clientEntity);
		} catch (Exception e)
		{
			log.debug("Can not retrieving client name attribute of the OAuth client", e);
			return;
		}

		reportFail(username != null ? username.getComparableValue() : null, clientName);
	}

	private String getClientName(EntityParam clientEntity)
	{
		String oauthGroup = oauthConfig.getValue(OAuthASProperties.CLIENTS_GROUP);
		Collection<AttributeExt> attrs;
		try
		{
			attrs = unsecureAttributesMan.getAllAttributes(clientEntity, true, oauthGroup, null, false);
		} catch (EngineException e)
		{
			throw new InternalException("Internal error, can not retrieve OAuth client's data", e);
		}

		Optional<AttributeExt> clientNameAttr = attrs.stream()
				.filter(a -> a.getName().equals(OAuthSystemAttributesProvider.CLIENT_NAME)).findFirst();
		if (clientNameAttr.isEmpty())
		{
			return null;
		}
		return clientNameAttr.get().getValues().get(0);
	}

	void reportFail(String clientUsername, String clientName)
	{
		report(clientUsername, clientName, Status.FAILED);
	}

	void reportSuccess(String clientUsername, String clientName, EntityParam owner)
	{
		report(clientUsername, clientName, Status.SUCCESSFUL);

		try
		{
			lastIdPClinetAccessAttributeManagement.setAttribute(owner, AccessProtocol.OAuth,
					new ApplicationId(clientUsername), Instant.now());
		} catch (EngineException e)
		{
			log.debug("Can not set last access attribute", e);
		}

	}

	private void report(String clientUsername, String clientName, Status status)
	{
		Endpoint endpoint = getEndpoint();
		eventPublisher.publishEvent(new IdpStatisticEvent(endpoint.getName(),
				endpoint.getConfiguration().getDisplayedName() != null
						? endpoint.getConfiguration().getDisplayedName().getValue(msg)
						: null,
				clientUsername, clientName, status));
	}

	private Endpoint getEndpoint()
	{
		if (authzEndpoint != null)
			return authzEndpoint;

		try
		{
			Optional<Endpoint> aendpoint = endpointMan.getEndpoints().stream().filter(
					e -> e.getConfiguration().getTag().equals(endpoint.getEndpoint().getConfiguration().getTag()))
					.findFirst();
			if (aendpoint.isPresent())
			{
				authzEndpoint = aendpoint.get();
				return authzEndpoint;
			} else
			{
				return endpoint.getEndpoint();
			}
		} catch (Exception e)
		{
			log.debug("Can not get relateed OAauth authz endpoint for token endpoint " + endpoint.getName(), e);
			return endpoint.getEndpoint();
		}
	}
}
