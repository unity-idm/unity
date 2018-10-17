/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_5;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.Update;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;
import pl.edu.icm.unity.store.objstore.realm.RealmHandler;

/**
 * Update db from 2.5 (Json schema V5) to 2.6.0+ (V5)
 * @author P.Piernik
 *
 */
@Component
public class JsonDumpUpdateFromV4 implements Update

{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV4.class);
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		updateEndpointConfiguration(contents);
		addAuthenticationFlow(contents);
		updateRealm(contents);
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void updateRealm(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				RealmHandler.REALM_OBJECT_TYPE))
		{
			UpdateHelperFrom2_5.updateRealm(objContent);
		}
	}

	private void addAuthenticationFlow(ObjectNode contents)
	{
		
		log.info("Add empty authentication flow array");
		contents.putArray("authenticationFlow");
	}

	private void updateEndpointConfiguration(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				EndpointHandler.ENDPOINT_OBJECT_TYPE))
		{
			UpdateHelperFrom2_5.updateEndpointConfiguration(objContent);
		}

	}
	
	private Set<ObjectNode> getGenericContent(ObjectNode contents, String type)
	{
		Set<ObjectNode> ret = new HashSet<>();
		ArrayNode generics = (ArrayNode) contents.get(type);
		if (generics != null)
		{
			for (JsonNode obj : generics)
			{
				ret.add((ObjectNode) obj.get("obj"));
			}
		}
		return ret;
	}
}
