/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_0;

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
 * Update JSon dump adding an empty files and certificates arrays. Note: it should be also matched by schema migration
 * with FILES table but that was forgotten and fixed as follows:
 *  - hotfix was implemented in the 3.1.1 release, adding the FILES table in DB if it is missing, without regular migration path. 
 *  - proper migration added in the update to the 3.2.0 unity version, and the hotfix was removed
 */
@Component
public class JsonDumpUpdateFromV7 implements JsonDumpUpdate
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV7.class);
	
	@Autowired
	private ObjectMapper objectMapper;


	@Override
	public int getUpdatedVersion()
	{
		return 7;
	}
	
	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
	
		ObjectNode newContents = insertFiles(contents);
		addCertificate(newContents);
		root.set("contents", newContents);
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	
	}

	private ObjectNode insertFiles(ObjectNode contents)
	{
		ObjectNode newContents = new ObjectNode(JsonNodeFactory.instance);
		Iterator<Map.Entry<String, JsonNode>> fields = contents.fields();
		while(fields.hasNext()){
		    Map.Entry<String, JsonNode> entry = fields.next();
		    newContents.putPOJO(entry.getKey(), entry.getValue());
		    if("attributes".equals(entry.getKey())){
			    log.info("Add empty files array");
			    newContents.putArray("files");
		    }
		}
		return newContents;
	}
	
	private void addCertificate(ObjectNode contents)
	{	
		log.info("Add empty certificate array");
		contents.putArray("certificate");
	}
}