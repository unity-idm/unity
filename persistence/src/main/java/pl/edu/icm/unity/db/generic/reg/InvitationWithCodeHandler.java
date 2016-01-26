/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.db.generic.reg;

import java.io.IOException;
import java.time.Instant;
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
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.Selection;
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
		ObjectNode json = mapper.createObjectNode();
		
		json.put("formId", value.getFormId());
		json.put("registrationCode", value.getRegistrationCode());
		json.put("expiration", value.getExpiration().getEpochSecond());
		if (value.getContactAddress() != null)
			json.put("contactAddress", value.getContactAddress());
		if (value.getChannelId() != null)
			json.put("channelId", value.getChannelId());
		json.set("identities", jsonMapper.valueToTree(value.getIdentities()));
		json.set("groupSelections", jsonMapper.valueToTree(value.getGroupSelections()));
		
		addAttributes(json, value.getAttributes());

		return json;
	}
	
	private InvitationWithCode fromJson(ObjectNode json, SqlSession sql) throws IOException
	{
		String formId = json.get("formId").asText();
		String registrationCode = json.get("registrationCode").asText();
		Instant expiration = Instant.ofEpochSecond(json.get("expiration").asLong());
		String addr = json.has("contactAddress") ? json.get("contactAddress").asText() : null;
		String channelId = json.has("channelId") ? json.get("channelId").asText() : null;
		InvitationWithCode invitation = new InvitationWithCode(formId, expiration,
				addr, channelId, registrationCode);
		
		JsonNode n;
		n = json.get("identities");
		fillIdentities((ObjectNode) n, invitation.getIdentities(), sql);

		n = json.get("groupSelections");
		fillGroups((ObjectNode) n, invitation.getGroupSelections(), sql);
		
		n = json.get("attributes");
		fillAttributes((ObjectNode) n, invitation.getAttributes(), sql);
		
		return invitation;
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
	
	private void fillAttributes(ObjectNode root, Map<Integer, PrefilledEntry<Attribute<?>>> attributes, 
			SqlSession sql)
	{
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
	}

	private void fillIdentities(ObjectNode root, Map<Integer, PrefilledEntry<IdentityParam>> identities, 
			SqlSession sql)
	{
		root.fields().forEachRemaining(field ->
		{
			ObjectNode el = (ObjectNode) field.getValue();
			IdentityParam identity;
			try
			{
				byte[] contents = jsonMapper.writeValueAsBytes(el.get("entry"));
				identity = jsonMapper.readValue(contents, IdentityParam.class);
			} catch (Exception e)
			{
				log.warn("Can not deserialize identity stored with invitation, ignoring it", e);
				return;
			}
			PrefilledEntryMode mode = PrefilledEntryMode.valueOf(el.get("mode").asText());
			identities.put(Integer.parseInt(field.getKey()), new PrefilledEntry<>(identity, mode));
		});
	}

	private void fillGroups(ObjectNode root, Map<Integer, PrefilledEntry<Selection>> groups, 
			SqlSession sql)
	{
		root.fields().forEachRemaining(field ->
		{
			ObjectNode el = (ObjectNode) field.getValue();
			Selection selection;
			try
			{
				byte[] contents = jsonMapper.writeValueAsBytes(el.get("entry"));
				selection = jsonMapper.readValue(contents, Selection.class);
			} catch (Exception e)
			{
				log.warn("Can not deserialize group stored with invitation, ignoring it", e);
				return;
			}
			PrefilledEntryMode mode = PrefilledEntryMode.valueOf(el.get("mode").asText());
			groups.put(Integer.parseInt(field.getKey()), new PrefilledEntry<>(selection, mode));
		});
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
