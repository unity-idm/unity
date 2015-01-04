/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.confirmations.ConfirmationFaciliity;
import pl.edu.icm.unity.server.api.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.server.api.confirmations.VerifiableElement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeExt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * VerifiableEmail verification facility.
 * 
 * @author P. Piernik
 */
public class AttributeFacility implements ConfirmationFaciliity
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributeFacility.class);
	private final ObjectMapper mapper = Constants.MAPPER;
	public static final String NAME = "attributeVerificator";
	private AttributesHelper attributesHelper;
	private DBSessionManager db;
	private DBAttributes dbAttributes;

	@Autowired
	public AttributeFacility(AttributesHelper attributesHelper, DBSessionManager db, DBAttributes dbAttributes)
	{
		this.attributesHelper = attributesHelper;
		this.db = db;
		this.dbAttributes = dbAttributes;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifi EmailAttribute";
	}

	public String prepareState(String entityId, String attrType, String group) throws EngineException
	{
		ObjectNode state = mapper.createObjectNode();
		state.with("confirmationState");
		state.put("entityId", entityId);
		state.put("attrType", attrType);
		state.put("group", group);
		state.put("verificator", getName());
		try
		{
			return mapper.writeValueAsString(state);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		Long entityId;
		String attrType;
		String group;
		try
		{
			ObjectNode main = mapper.readValue(state, ObjectNode.class);
			entityId = main.get("entityId").asLong();
			attrType = main.get("attrType").asText();
			group = main.get("group").asText();

		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		
		SqlSession sql = db.getSqlSession(false);	
		try	
		{
			//TODO CHECK AUTHZ!!!	
			Collection<AttributeExt<?>> all = attributesHelper.getAllAttributesInternal(sql, entityId, false, group, attrType, false);
			for (AttributeExt<?> a : all)
			{
				VerifiableElement el = (VerifiableElement) a.getValues().get(0);
				el.setVerified(true);
				dbAttributes.addAttribute(entityId, a, true, sql);
				log.trace("Confirm attribute " + a.getName() + " in entity " + entityId);
			}
			sql.commit();
		}finally
		{
			db.releaseSqlSession(sql);
		}
		//TODO 
		return new ConfirmationStatus(true, "SUCCESSFULL CONFIRM ATTRIBUTE " + attrType);
		
	}

}
