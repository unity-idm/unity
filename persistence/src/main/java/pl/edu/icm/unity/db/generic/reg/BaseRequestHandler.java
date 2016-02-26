/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.json.FullAttributeSerializer;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.UserRequestState;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base of handlers of {@link RegistrationRequestState} and {@link EnquiryResponseState}.
 * 
 * @author K. Benedyczak
 */
public abstract class BaseRequestHandler<R extends BaseRegistrationInput, T extends UserRequestState<R>> 
	extends DefaultEntityHandler<T>
{
	private FullAttributeSerializer attributeSerializer;
	protected ObjectMapper jsonMapper = new ObjectMapper();
	
	public BaseRequestHandler(ObjectMapper jsonMapper, String type, Class<T> clazz,
			FullAttributeSerializer attributeSerializer)
	{
		super(jsonMapper, type, clazz);
		this.attributeSerializer = attributeSerializer;
	}

	@Override
	public GenericObjectBean toBlob(T value, SqlSession sql)
	{
		try
		{
			ObjectNode root = toObjectNode(value, sql);
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getRequestId(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration request to JSON", e);
		}
	}

	protected ObjectNode toObjectNode(T value, SqlSession sql)
	{
		ObjectNode root = jsonMapper.createObjectNode();
		root.set("AdminComments", jsonMapper.valueToTree(value.getAdminComments()));
		root.set("RequestId", jsonMapper.valueToTree(value.getRequestId()));
		root.set("Status", jsonMapper.valueToTree(value.getStatus()));
		root.set("Timestamp", jsonMapper.valueToTree(value.getTimestamp().getTime()));
		root.set("Context", value.getRegistrationContext().toJson());
		R req = value.getRequest();
		root.set("Agreements", jsonMapper.valueToTree(req.getAgreements()));
		addAttributes(root, req.getAttributes());
		root.set("Comments", jsonMapper.valueToTree(req.getComments()));
		root.set("Credentials", jsonMapper.valueToTree(req.getCredentials()));
		root.set("FormId", jsonMapper.valueToTree(req.getFormId()));
		root.set("GroupSelections", jsonMapper.valueToTree(req.getGroupSelections()));
		root.set("Identities", jsonMapper.valueToTree(req.getIdentities()));
		root.put("UserLocale", req.getUserLocale());
		return root;
	}

	private void addAttributes(ObjectNode root, List<Attribute<?>> attributes)
	{
		ArrayNode jsonAttrs = root.putArray("Attributes");
		for (Attribute<?> a: attributes)
		{
			if (a == null)
			{
				jsonAttrs.add((ObjectNode)null);
				continue;
			}
			ObjectNode ap = jsonMapper.createObjectNode();
			ap.set("attribute", attributeSerializer.toJson(a));
			jsonAttrs.add(ap);
		}
	}
	
	private List<Attribute<?>> getAttributes(ArrayNode n, SqlSession sql) 
			throws IllegalAttributeTypeException, IllegalTypeException
	{
		List<Attribute<?>> ret = new ArrayList<>(n.size());
		for (int i=0; i<n.size(); i++)
		{
			JsonNode node = n.get(i);
			if (node instanceof NullNode)
			{
				ret.add(null);
				continue;
			}
			ObjectNode el = (ObjectNode) node;
			Attribute<?> a = attributeSerializer.fromJson((ObjectNode) el.get("attribute"), sql);
			ret.add(a);
		}
		return ret;
	}
	
	@Override
	public T fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			return fromObjectNode(root, sql);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration request from JSON", e);
		}
	}
	
	protected abstract T fromObjectNode(ObjectNode root, SqlSession sql);
	
	protected void parsePreamble(T ret, ObjectNode root, SqlSession sql)
	{
		try
		{
			JsonNode n = root.get("AdminComments");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<AdminComment> r = jsonMapper.readValue(v, 
						new TypeReference<List<AdminComment>>(){});
				ret.setAdminComments(r);
			}

			n = root.get("RequestId");
			ret.setRequestId(n.asText());

			n = root.get("Status");
			ret.setStatus(RegistrationRequestStatus.valueOf(n.asText()));

			n = root.get("Timestamp");
			ret.setTimestamp(new Date(n.longValue()));

			n = root.get("Context");
			if (n != null)
				ret.setRegistrationContext(RegistrationContext.fromJson(n));
			else
				ret.setRegistrationContext(new RegistrationContext(true, true, 
						TriggeringMode.manualAtLogin));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize request base state from JSON", e);
		}
	}

	protected void parseRequest(R retReq, ObjectNode root, SqlSession sql)
	{
		try
		{
			JsonNode n = root.get("Agreements");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<Selection> r = jsonMapper.readValue(v, 
						new TypeReference<List<Selection>>(){});
				retReq.setAgreements(r);
			}
			
			n = root.get("Attributes");
			retReq.setAttributes(getAttributes((ArrayNode) n, sql));

			n = root.get("Comments");
			if (n != null && !n.isNull())
				retReq.setComments(n.asText());
			
			n = root.get("Credentials");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<CredentialParamValue> r = jsonMapper.readValue(v, 
						new TypeReference<List<CredentialParamValue>>(){});
				retReq.setCredentials(r);
			}
			
			n = root.get("FormId");
			retReq.setFormId(n.asText());

			n = root.get("GroupSelections");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<Selection> r = jsonMapper.readValue(v, 
						new TypeReference<List<Selection>>(){});
				retReq.setGroupSelections(r);
			}

			n = root.get("Identities");			
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				List<IdentityParam> r = jsonMapper.readValue(v, 
						new TypeReference<List<IdentityParam>>(){});
				retReq.setIdentities(r);
			}
			
			n = root.get("UserLocale");
			if (n != null && !n.isNull())
				retReq.setUserLocale(n.asText());
			
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize request contents from JSON", e);
		}
	}
}
