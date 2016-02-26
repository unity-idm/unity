/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.Iterator;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.FullAttributeSerializer;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@link RegistrationRequestState}
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestHandler extends BaseRequestHandler<RegistrationRequest, RegistrationRequestState>
{
	public static final String REGISTRATION_REQUEST_OBJECT_TYPE = "registrationRequest";
	
	@Autowired
	public RegistrationRequestHandler(ObjectMapper jsonMapper, FullAttributeSerializer attributeSerializer)
	{
		super(jsonMapper, REGISTRATION_REQUEST_OBJECT_TYPE, RegistrationRequestState.class, 
				attributeSerializer);
	}

	@Override
	protected RegistrationRequestState fromObjectNode(ObjectNode root, SqlSession sql)
	{
		RegistrationRequestState ret = new RegistrationRequestState();
		parsePreamble(ret, root, sql);
		RegistrationRequest retReq = new RegistrationRequest();
		ret.setRequest(retReq);
		parseRequest(retReq, root, sql);
		
		JsonNode n = root.get("RegistrationCode");
		if (n != null && !n.isNull())
			retReq.setRegistrationCode(n.asText());
		
		return ret;
	}
	
	@Override
	protected ObjectNode toObjectNode(RegistrationRequestState value, SqlSession sql)
	{
		ObjectNode ret = super.toObjectNode(value, sql);
		ret.set("RegistrationCode", jsonMapper.valueToTree(value.getRequest().getRegistrationCode()));
		return ret;
	}

	@Override
	public byte[] updateBeforeImport(String name, JsonNode node) throws JsonProcessingException
	{
		if (node == null)
			return null;
		
		if (node.has("Identities"))
		{
			ArrayNode ids = (ArrayNode) node.get("Identities");
			Iterator<JsonNode> idsIt = ids.iterator();
			while (idsIt.hasNext())
			{
				ObjectNode id = (ObjectNode) idsIt.next();
				JsonNode extIdp = id.remove("externalIdp");
				if (extIdp != null)
					id.set("remoteIdp", extIdp);
			}
		}
		return jsonMapper.writeValueAsBytes(node);
	}
}
