/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.realm;

import java.io.IOException;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Handler for {@link AuthenticationRealm}s storage.
 * @author K. Benedyczak
 */
@Component
public class RealmHandler extends DefaultEntityHandler<AuthenticationRealm>
{
	public static final String REALM_OBJECT_TYPE = "authenticationRealm";
	
	@Autowired
	public RealmHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, REALM_OBJECT_TYPE, AuthenticationRealm.class);
	}

	@Override
	public GenericObjectBean toBlob(AuthenticationRealm value, SqlSession sql)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.put("allowForRememberMeDays", value.getAllowForRememberMeDays());
			root.put("blockAfterUnsuccessfulLogins", value.getBlockAfterUnsuccessfulLogins());
			root.put("blockFor", value.getBlockFor());
			root.put("maxInactivity", value.getMaxInactivity());
			root.put("description", value.getDescription());
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize to JSON authentication realm state", e);
		}
	}

	@Override
	public AuthenticationRealm fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			JsonNode root = jsonMapper.readTree(blob.getContents());
			int allowForRememberMeDays = root.get("allowForRememberMeDays").asInt();
			int blockAfterUnsuccessfulLogins = root.get("blockAfterUnsuccessfulLogins").asInt();
			int blockFor = root.get("blockFor").asInt();
			int maxInactivity = root.get("maxInactivity").asInt();
			String description = root.get("description").asText();
			return new AuthenticationRealm(blob.getName(), description, blockAfterUnsuccessfulLogins,
					blockFor, allowForRememberMeDays, maxInactivity);
		} catch (IOException e)
		{
			throw new InternalException("Can't deserialize JSON authentication realm state", e);
		}
	}
}
