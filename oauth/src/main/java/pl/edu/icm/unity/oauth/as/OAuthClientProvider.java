/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;

@Component
public class OAuthClientProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthClientProvider.class);
	private EntityManagement identitiesMan;
	private AttributesManagement attributesMan;

	public OAuthClientProvider(@Qualifier("insecure") EntityManagement identitiesManagement,
			@Qualifier("insecure") AttributesManagement attributesManagement)
	{
		this.identitiesMan = identitiesManagement;
		this.attributesMan = attributesManagement;
	}

	public OAuthClient getClient(long entityId, String oauthGroup) throws OAuthValidationException
	{
		EntityParam clientEntity = new EntityParam(entityId);
		Entity clientResolvedEntity;
		try
		{
			clientResolvedEntity = identitiesMan.getEntity(clientEntity);

		} catch (IllegalArgumentException e)
		{
			throw new OAuthValidationException("The client '" + entityId + "' is unknown");
		} catch (Exception e)
		{
			log.error("Problem retrieving identity of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}

		Identity username = clientResolvedEntity.getIdentities().stream()
				.filter(i -> i.getTypeId().equals(UsernameIdentity.ID)).findFirst().orElse(null);

		return new OAuthClient(username != null ? username.getComparableValue() : null,

				clientResolvedEntity.getId(), getAttributesNoAuthZ(clientEntity, oauthGroup));

	}

	public OAuthClient getClient(String username, String oauthGroup) throws OAuthValidationException
	{
		EntityParam clientEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, username));
		Entity clientResolvedEntity;
		try
		{
			clientResolvedEntity = identitiesMan.getEntity(clientEntity);

		} catch (IllegalArgumentException e)
		{
			throw new OAuthValidationException("The client '" + username + "' is unknown");
		} catch (Exception e)
		{
			log.error("Problem retrieving identity of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}

		return new OAuthClient(username, clientResolvedEntity.getId(), getAttributesNoAuthZ(clientEntity, oauthGroup));

	}

	private Map<String, AttributeExt> getAttributesNoAuthZ(EntityParam clientEntity, String oauthGroup)
			throws OAuthValidationException
	{

		Collection<AttributeExt> attrs;
		try
		{
			attrs = attributesMan.getAllAttributes(clientEntity, true, oauthGroup, null, false);
		} catch (EngineException e)
		{
			log.error("Problem retrieving attributes of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}
		Map<String, AttributeExt> ret = new HashMap<>();
		attrs.stream().forEach(a -> ret.put(a.getName(), a));
		return ret;
	}

}
