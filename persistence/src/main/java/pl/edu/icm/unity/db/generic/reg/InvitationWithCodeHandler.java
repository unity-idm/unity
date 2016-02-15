/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.json.FullAttributeSerializer;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serialization and deserialization of {@link InvitationWithCode}.
 * 
 * @author Krzysztof Benedyczak
 */
@Component
public class InvitationWithCodeHandler extends DefaultEntityHandler<InvitationWithCode>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InvitationWithCodeHandler.class);
	public static final String INVITATION_OBJECT_TYPE = "invitationWithCode";
	private FullAttributeSerializer attributeSerializer;
	
	@Autowired
	public InvitationWithCodeHandler(ObjectMapper jsonMapper, FullAttributeSerializer attributeSerializer)
	{
		super(jsonMapper, INVITATION_OBJECT_TYPE, InvitationWithCode.class);
		this.attributeSerializer = attributeSerializer;
	}

	private ObjectNode toJson(InvitationWithCode value, ObjectMapper mapper)
	{
		ObjectNode json = value.toJson();
		addAttributes(json, value.getAttributes());
		return json;
	}
	
	private InvitationWithCode fromJson(ObjectNode json, SqlSession sql) throws IOException
	{
		JsonNode n = json.get("attributes");
		Map<Integer, PrefilledEntry<Attribute<?>>> attributes = readAttributes((ObjectNode) n, sql);
		return new InvitationWithCode(json, attributes);
	}

	private void addAttributes(ObjectNode root, Map<Integer, PrefilledEntry<Attribute<?>>> attributes)
	{
		ObjectNode jsonAttrs = root.putObject("attributes");
		for (Map.Entry<Integer, PrefilledEntry<Attribute<?>>> ae: attributes.entrySet())
		{
			ObjectNode entry = jsonMapper.createObjectNode();
			entry.set("attribute", attributeSerializer.toJson(ae.getValue().getEntry()));
			entry.put("mode", ae.getValue().getMode().name());
			jsonAttrs.set(ae.getKey().toString(), entry);
		}
	}
	
	private Map<Integer, PrefilledEntry<Attribute<?>>> readAttributes(ObjectNode root, SqlSession sql)
	{
		Map<Integer, PrefilledEntry<Attribute<?>>> attributes = new HashMap<>();
		root.fields().forEachRemaining(field ->
		{
			ObjectNode el = (ObjectNode) field.getValue();
			Attribute<?> a;
			try
			{
				a = attributeSerializer.fromJson((ObjectNode) el.get("attribute"), sql);
			} catch (Exception e)
			{
				log.warn("Can not deserialize attribute stored with invitation, ignoring it", e);
				return;
			}
			PrefilledEntryMode mode = PrefilledEntryMode.valueOf(el.get("mode").asText());
			attributes.put(Integer.parseInt(field.getKey()), new PrefilledEntry<Attribute<?>>(a, mode));
		});
		return attributes;
	}

	@Override
	public GenericObjectBean toBlob(InvitationWithCode value, SqlSession sql)
	{
		try
		{
			ObjectNode root = toJson(value, jsonMapper);
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getRegistrationCode(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration invitation to JSON", e);
		}
	}

	@Override
	public InvitationWithCode fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			return fromJson(root, sql);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration invitation from JSON", e);
		}
	}
}
