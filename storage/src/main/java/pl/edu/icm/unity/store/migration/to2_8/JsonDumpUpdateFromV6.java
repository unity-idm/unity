/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to2_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.export.JsonDumpUpdate;

/**
 * Update JSon dump from V6 version, see {@link UpdateHelperTo2_8}
 */
@Component
public class JsonDumpUpdateFromV6 implements JsonDumpUpdate
{
	@Autowired
	private ObjectMapper objectMapper;


	@Override
	public int getUpdatedVersion()
	{
		return 6;
	}
	
	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		updateAuthenticators(contents);
		updateRegistrationRequest(contents);
		updateEnquiryResponse(contents);
		updateInvitationWithCode(contents);
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}
	
	private void updateInvitationWithCode(ObjectNode contents)
	{
		genericObjectUpdate("invitationWithCode", contents, UpdateHelperTo2_8::updateInvitationWithCode);
	}

	private void updateAuthenticators(ObjectNode contents)
	{
		genericObjectUpdate("authenticator", contents, UpdateHelperTo2_8::updateAuthenticator);
	}
	
	private void updateRegistrationRequest(ObjectNode contents)
	{
		genericObjectUpdate("registrationRequest", contents, UpdateHelperTo2_8::updateRegistrationRequest);
	}

	private void updateEnquiryResponse(ObjectNode contents)
	{
		genericObjectUpdate("enquiryResponse", contents, UpdateHelperTo2_8::updateEnquiryResponse);
	}
	
	private void genericObjectUpdate(String type, ObjectNode contents, GenericObjectDataUpdater updater)
	{
		ArrayNode generics = (ArrayNode) contents.get(type);
		if (generics == null)
			return;
		
		Iterator<JsonNode> elements = generics.elements();
		
		while (elements.hasNext())
		{
			ObjectNode next = (ObjectNode) elements.next();
			ObjectNode genericObject = (ObjectNode)next.get("obj");
			ObjectNode updated = updater.update(genericObject);
			next.set("obj", updated);
		}
	}
}
