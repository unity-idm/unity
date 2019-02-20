/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.preferences;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
@Primary
@Transactional
public class PreferencesManagementImpl implements PreferencesManagement
{
	private ObjectMapper mapper;
	private AttributeDAO dbAttributes;
	private InternalAuthorizationManager authz;
	private EntityResolver idResolver;
	
	@Autowired
	public PreferencesManagementImpl(AttributeSyntaxFactoriesRegistry attrValueTypesReg,
			AttributeDAO dbAttributes, 
			EntityResolver idResolver, InternalAuthorizationManager authz, ObjectMapper mapper)
	{
		this.dbAttributes = dbAttributes;
		this.idResolver = idResolver;
		this.authz = authz;
		this.mapper = mapper;
	}

	@Override
	public void setPreference(EntityParam entity, String preferenceId, String value)
			throws EngineException
	{
		if (preferenceId == null)
			throw new IllegalAttributeValueException("Preference must not be null");
		if (value == null)
			throw new IllegalAttributeValueException("Preference value must not be null");
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), 
				AuthzCapability.attributeModify);
		String raw = getPreferenceAttribute(entityId);
		String updated = setPreference(raw, preferenceId, value);
		storePreferenceAttribute(entityId, updated, raw != null);
	}

	@Override
	public String getPreference(EntityParam entity, String preferenceId) throws EngineException
	{
		if (preferenceId == null)
			throw new IllegalAttributeValueException("Preference must not be null");
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), 
				AuthzCapability.read);
		String raw = getPreferenceAttribute(entityId);
		return extractPreference(raw, preferenceId);
	}

	@Override
	public void removePreference(EntityParam entity, String preferenceId)
			throws EngineException
	{
		if (preferenceId == null)
			throw new IllegalAttributeValueException("Preference must not be null");
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), 
				AuthzCapability.attributeModify);
		String raw = getPreferenceAttribute(entityId);
		String updated = setPreference(raw, preferenceId, null);
		storePreferenceAttribute(entityId, updated, raw != null);
	}
	
	private String getPreferenceAttribute(long entityId) 
			throws EngineException
	{
		Collection<AttributeExt> preference = dbAttributes.getEntityAttributes(entityId,
				PreferencesAttributeTypeProvider.PREFERENCES, "/");
		if (preference.size() == 0)
			return null;
		AttributeExt attr = preference.iterator().next();
		List<?> values = attr.getValues();
		return values.size() > 0 ? (String)values.get(0) : null;
	}
	
	private void storePreferenceAttribute(long entityId, String value, boolean update) 
			throws IllegalAttributeValueException, IllegalTypeException, IllegalAttributeTypeException, IllegalGroupValueException
	{
		Attribute sa = StringAttribute.of(PreferencesAttributeTypeProvider.PREFERENCES, "/", value);
		AttributeExt atExt = new AttributeExt(sa, true, new Date(), new Date());
		if (update)
			dbAttributes.updateAttribute(new StoredAttribute(atExt, entityId));
		else
			dbAttributes.create(new StoredAttribute(atExt, entityId));
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
