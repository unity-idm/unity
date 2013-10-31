/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.json.FullAttributeSerializer;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.AttributeParamValue;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.Selection;

/**
 * Handler for {@link RegistrationRequestState}
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestHandler extends DefaultEntityHandler<RegistrationRequestState>
{
	public static final String REGISTRATION_REQUEST_OBJECT_TYPE = "registrationRequest";
	private FullAttributeSerializer attributeSerializer;
	
	@Autowired
	public RegistrationRequestHandler(ObjectMapper jsonMapper, FullAttributeSerializer attributeSerializer)
	{
		super(jsonMapper, REGISTRATION_REQUEST_OBJECT_TYPE, RegistrationRequestState.class);
		this.attributeSerializer = attributeSerializer;
	}

	@Override
	public GenericObjectBean toBlob(RegistrationRequestState value, SqlSession sql)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.put("AdminComments", jsonMapper.valueToTree(value.getAdminComments()));
			root.put("RequestId", jsonMapper.valueToTree(value.getRequestId()));
			root.put("Status", jsonMapper.valueToTree(value.getStatus()));
			root.put("Timestamp", jsonMapper.valueToTree(value.getTimestamp().getTime()));
			RegistrationRequest req = value.getRequest();
			root.put("Agreements", jsonMapper.valueToTree(req.getAgreements()));
			addAttributes(root, req.getAttributes());
			root.put("Comments", jsonMapper.valueToTree(req.getComments()));
			root.put("Credentials", jsonMapper.valueToTree(req.getCredentials()));
			root.put("FormId", jsonMapper.valueToTree(req.getFormId()));
			root.put("GroupSelections", jsonMapper.valueToTree(req.getGroupSelections()));
			root.put("Identities", jsonMapper.valueToTree(req.getIdentities()));
			root.put("RegistrationCode", jsonMapper.valueToTree(req.getRegistrationCode()));

			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getRequestId(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration request to JSON", e);
		}
	}

	private void addAttributes(ObjectNode root, List<AttributeParamValue> attributes)
	{
		ArrayNode jsonAttrs = root.putArray("Attributes");
		for (AttributeParamValue a: attributes)
		{
			ObjectNode ap = jsonMapper.createObjectNode();
			ap.set("attribute", attributeSerializer.toJson(a.getAttribute()));
			ap.put("external", a.isExternal());
			jsonAttrs.add(ap);
		}
	}
	
	private List<AttributeParamValue> getAttributes(ArrayNode n, SqlSession sql) 
			throws IllegalAttributeTypeException, IllegalTypeException
	{
		List<AttributeParamValue> ret = new ArrayList<>(n.size());
		for (int i=0; i<n.size(); i++)
		{
			ObjectNode el = (ObjectNode) n.get(i);
			AttributeParamValue av = new AttributeParamValue();
			av.setExternal(el.get("external").asBoolean());
			Attribute<?> a = attributeSerializer.fromJson((ObjectNode) el.get("attribute"), sql);
			av.setAttribute(a);
			ret.add(av);
		}
		return ret;
	}
	
	@Override
	public RegistrationRequestState fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			ObjectNode root = (ObjectNode) jsonMapper.readTree(blob.getContents());
			RegistrationRequestState ret = new RegistrationRequestState();
			
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

			RegistrationRequest retReq = new RegistrationRequest();
			ret.setRequest(retReq);
			
			n = root.get("Agreements");
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
			
			n = root.get("RegistrationCode");
			if (n != null && !n.isNull())
				retReq.setRegistrationCode(n.asText());
			return ret;
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration request from JSON", e);
		}
	}
}
