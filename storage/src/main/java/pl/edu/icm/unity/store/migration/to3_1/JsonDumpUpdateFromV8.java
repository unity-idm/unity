/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

/**
 * Adds empty audit events array to dump. 
 */
@Component
public class JsonDumpUpdateFromV8 implements JsonDumpUpdate
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV8.class);
	
	@Autowired
	private ObjectMapper objectMapper;


	@Override
	public int getUpdatedVersion()
	{
		return 8;
	}
	
	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");

		ObjectNode newContents = insertAuditEvents(contents);
		root.set("contents", newContents);

		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));

	}

	private ObjectNode insertAuditEvents(ObjectNode contents)
	{
		ObjectNode newContents = new ObjectNode(JsonNodeFactory.instance);
		Iterator<Map.Entry<String, JsonNode>> fields = contents.fields();
		while(fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			newContents.putPOJO(entry.getKey(), entry.getValue());
			if ("files".equals(entry.getKey())){
				log.info("Add empty auditEvents array");
				newContents.putArray("auditEvents");
			}
		}
		return newContents;
	}
}