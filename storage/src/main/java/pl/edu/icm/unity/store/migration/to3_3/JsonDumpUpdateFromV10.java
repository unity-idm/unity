/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * Update JSon dump adding an empty policyDocumentsarrays if header not contain
 * dumpElements.
 * 
 * @author P.Piernik
 *
 */
@Component
public class JsonDumpUpdateFromV10 implements JsonDumpUpdate
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV10.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 10;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);

		ObjectNode contents = (ObjectNode) root.get("contents");
		JsonNode dumpElements = root.get("dumpElements");
		if (dumpElements == null)
		{
			ObjectNode newContents = insertPolicyDocuments(contents);
			root.set("contents", newContents);
		}

		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));

	}

	private ObjectNode insertPolicyDocuments(ObjectNode contents)
	{
		ObjectNode newContents = new ObjectNode(JsonNodeFactory.instance);
		Iterator<Map.Entry<String, JsonNode>> fields = contents.fields();
		while (fields.hasNext())
		{
			Map.Entry<String, JsonNode> entry = fields.next();
			newContents.putPOJO(entry.getKey(), entry.getValue());
			if ("auditEvents".equals(entry.getKey()))
			{
				log.info("Add empty policy documents array");
				newContents.putArray("policyDocuments");
			}
		}
		return newContents;
	}

}