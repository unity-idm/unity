/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.cred;

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
import pl.edu.icm.unity.server.utils.I18nStringJsonUtil;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Handler for {@link CredentialDefinition}
 * @author K. Benedyczak
 */
@Component
public class CredentialHandler extends DefaultEntityHandler<CredentialDefinition>
{
	public static final String CREDENTIAL_OBJECT_TYPE = "credential";
	
	@Autowired
	public CredentialHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, CREDENTIAL_OBJECT_TYPE, CredentialDefinition.class);
	}

	@Override
	public GenericObjectBean toBlob(CredentialDefinition value, SqlSession sql)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			
			root.put("typeId", value.getTypeId());
			root.put("name", value.getName());
			root.put("jsonConfiguration", value.getJsonConfiguration());
			root.set("displayedName", I18nStringJsonUtil.toJson(value.getDisplayedName()));
			root.set("i18nDescription", I18nStringJsonUtil.toJson(value.getDescription()));
			
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize credential to JSON", e);
		}
	}

	@Override
	public CredentialDefinition fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			CredentialDefinition ret = new CredentialDefinition();
			
			JsonNode n;
			
			n = root.get("name");
			ret.setName(n.asText());

			n = root.get("typeId");
			ret.setTypeId(n.asText());
			
			n = root.get("jsonConfiguration");
			ret.setJsonConfiguration(n.asText());

			if (root.has("displayedName"))
				ret.setDisplayedName(I18nStringJsonUtil.fromJson(root.get("displayedName")));
			else
				ret.setDisplayedName(new I18nString(ret.getName()));
	
			ret.setDescription(I18nStringJsonUtil.fromJson(root.get("i18nDescription"), 
					root.get("description")));
			
			return ret;
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize credential from JSON", e);
		}
	}
}
