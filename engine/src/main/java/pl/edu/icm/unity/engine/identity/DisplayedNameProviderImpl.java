/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.DisplayedNameProvider;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;

//TODO authZ
@Component
public class DisplayedNameProviderImpl implements DisplayedNameProvider
{
	private static final String[] PREFERRED_IDENTITY_TYPES = {
			EmailIdentity.ID, 
			UsernameIdentity.ID, 
			X500Identity.ID, 
			PersistentIdentity.ID};
	
	private final AttributesHelper attributesHelper;
	private final IdentityDAO idDAO;
	private final EntityResolver idResolver;

	@Autowired
	public DisplayedNameProviderImpl(AttributesHelper attributesHelper, IdentityDAO idDAO,
			EntityResolver idResolver)
	{
		this.attributesHelper = attributesHelper;
		this.idDAO = idDAO;
		this.idResolver = idResolver;
	}

	@Override
	public String getDisplayedName(EntityParam entity) throws EngineException
	{
		long entityId = idResolver.getEntityId(entity);
		AttributeType displayedNameAttr = attributesHelper.getAttributeTypeWithSingeltonMetadata(//TODO can be cached
				EntityNameMetadataProvider.NAME);
		if (displayedNameAttr != null)
		{
			String nameFromAttribute = getDisplayedNameFromAttribute(entityId, displayedNameAttr.getName());
			if (nameFromAttribute != null)
				return nameFromAttribute;
		}
		return getDisplayedNameFromIdentity(entityId);
	}

	@Override
	public Map<Long, String> getDisplayedNamesInGroup(String group) throws EngineException
	{
		AttributeType displayedNameAttr = attributesHelper.getAttributeTypeWithSingeltonMetadata(//TODO can be cached
				EntityNameMetadataProvider.NAME);
		
		// TODO Auto-generated method stub
		return null;
	}

	private String getDisplayedNameFromIdentity(long entityId) throws EngineException
	{
		Map<String, Identity> byType = getIdentitiesForEntity(entityId);
		for (String pref: PREFERRED_IDENTITY_TYPES)
		{
			Identity identity = byType.get(pref);
			if (identity != null)
				return identity.getValue();
		}
		return null;
	}
	
	private Map<String, Identity> getIdentitiesForEntity(long entityId) 
			throws IllegalTypeException
	{
		List<Identity> all = idDAO.getByEntity(entityId);
		Map<String, Identity> ret = new HashMap<>();
		for (Identity id: all)
			if (id.getTarget() == null)
				ret.put(id.getTypeId(), id);
		return ret;
	}
	
	private String getDisplayedNameFromAttribute(long entityId, String displayedNameAttr) throws EngineException
	{
		AttributeExt attribute = getDisplayedNameAttr(entityId, displayedNameAttr);
		if (attribute == null)
			return null;
		List<?> values = attribute.getValues();
		if (values.isEmpty())
			return null;
		return values.get(0).toString();
	}
	
	private AttributeExt getDisplayedNameAttr(long entityId, String displayedNameAttr) throws EngineException
	{
		Collection<AttributeExt> ret = attributesHelper.getAllAttributes(entityId, "/", true, displayedNameAttr);
		return ret.size() == 1 ? ret.iterator().next() : null; 
	}
}
