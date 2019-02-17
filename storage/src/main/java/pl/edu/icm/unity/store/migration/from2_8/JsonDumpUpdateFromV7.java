/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.Update;
import pl.edu.icm.unity.store.migration.from2_5.JsonDumpUpdateFromV4;

/**
 * Update JSon dump from V7 version, see {@link UpdateHelperFrom2_8}
 * @author P.Piernik
 *
 */
@Component
public class JsonDumpUpdateFromV7 implements Update
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV4.class);
	
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		addCertificate(contents);		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	
	}

	private void addCertificate(ObjectNode contents)
	{	
		log.info("Add empty certificate array");
		contents.putArray("certificate");
	}
}