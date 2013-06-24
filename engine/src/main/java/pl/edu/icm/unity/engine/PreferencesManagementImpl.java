/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
public class PreferencesManagementImpl implements PreferencesManagement
{
	private ObjectMapper mapper;
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	private IdentitiesResolver idResolver;
	private AuthorizationManager authz;
	
	@Autowired
	public PreferencesManagementImpl(AttributeSyntaxFactoriesRegistry attrValueTypesReg,
			DBSessionManager db, DBAttributes dbAttributes, 
			IdentitiesResolver idResolver, AuthorizationManager authz, ObjectMapper mapper)
	{
		this.db = db;
		this.dbAttributes = dbAttributes;
		this.idResolver = idResolver;
		this.authz = authz;
		this.mapper = mapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPreference(EntityParam entity, String preferenceId, String value)
			throws EngineException
	{
		if (preferenceId == null)
			throw new IllegalAttributeValueException("Preference must not be null");
		if (value == null)
			throw new IllegalAttributeValueException("Preference value must not be null");
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(authz.isSelf(entityId), 
					AuthzCapability.attributeModify);
			String raw = getPreferenceAttribute(entityId, sql);
			String updated = setPreference(raw, preferenceId, value);
			storePreferenceAttribute(entityId, updated, sql);
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPreference(EntityParam entity, String preferenceId) throws EngineException
	{
		if (preferenceId == null)
			throw new IllegalAttributeValueException("Preference must not be null");
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(authz.isSelf(entityId), 
					AuthzCapability.read);
			String raw = getPreferenceAttribute(entityId, sql);
			sql.commit();
			return extractPreference(raw, preferenceId);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removePreference(EntityParam entity, String preferenceId)
			throws EngineException
	{
		if (preferenceId == null)
			throw new IllegalAttributeValueException("Preference must not be null");
		entity.validateInitialization();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sql);
			authz.checkAuthorization(authz.isSelf(entityId), 
					AuthzCapability.attributeModify);
			String raw = getPreferenceAttribute(entityId, sql);
			String updated = setPreference(raw, preferenceId, null);
			storePreferenceAttribute(entityId, updated, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private String getPreferenceAttribute(long entityId, SqlSession sql) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		Collection<AttributeExt<?>> preference = dbAttributes.getAllAttributes(entityId, "/", false, 
				SystemAttributeTypes.PREFERENCES, sql);
		if (preference.size() == 0)
			return null;
		AttributeExt<?> attr = preference.iterator().next();
		List<?> values = attr.getValues();
		return values.size() > 0 ? (String)values.get(0) : null;
	}
	
	private void storePreferenceAttribute(long entityId, String value, SqlSession sql) 
			throws IllegalAttributeValueException, IllegalTypeException, IllegalAttributeTypeException, IllegalGroupValueException
	{
		StringAttribute sa = new StringAttribute(SystemAttributeTypes.PREFERENCES, 
				"/", AttributeVisibility.local, value);
		dbAttributes.addAttribute(entityId, sa, true, sql);
	}
	
	private String extractPreference(String raw, String preferenceId)
	{
		ObjectNode main = parseOrCreate(raw);
		JsonNode ret = main.get(preferenceId);
		return ret == null ? null : ret.asText();
	}
	
	private String setPreference(String currentRaw, String preferenceId, String newValue)
	{
		ObjectNode main = parseOrCreate(currentRaw);
		if (newValue != null)
			main.put(preferenceId, newValue);
		else
			main.remove(preferenceId);
		try
		{
			return mapper.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	private ObjectNode parseOrCreate(String currentRaw)
	{
		if (currentRaw == null)
			return mapper.createObjectNode();
		try
		{
			return mapper.readValue(currentRaw, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}
}
