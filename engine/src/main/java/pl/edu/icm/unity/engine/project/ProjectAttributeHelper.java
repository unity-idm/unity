/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * A collection of methods useful in projects management impl
 * @author P.Piernik
 *
 */
@Component
class ProjectAttributeHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, ProjectAttributeHelper.class);

	private final AttributesManagement attrMan;
	private final AttributesHelper attrHelper;
	private final AttributeTypeHelper atHelper;

	@Autowired
	ProjectAttributeHelper(@Qualifier("insecure")AttributesManagement attrMan, AttributesHelper attrHelper,
			AttributeTypeHelper atHelper)
	{
		this.attrMan = attrMan;
		this.attrHelper = attrHelper;
		this.atHelper = atHelper;
	}

	public Optional<Attribute> getAttribute(long entityId, String path, String attribute) throws EngineException
	{
		Collection<AttributeExt> attributes = attrMan.getAttributes(new EntityParam(entityId), path, attribute);

		if (!attributes.isEmpty())
		{
			return Optional.ofNullable(attributes.iterator().next());
		}
		return Optional.empty();
	}

	public Optional<String> getAttributeValue(long entityId, String path, String attribute) throws EngineException
	{

		Optional<Attribute> attr = getAttribute(entityId, path, attribute);
		if (attr.isPresent())
		{
			if (!attr.get().getValues().isEmpty())
			{
				return Optional.ofNullable(attr.get().getValues().iterator().next());
			}
		}
		return Optional.empty();
	}

	private VerifiableElementBase getVerifiableAttributeValue(String attributeName, String value)
	{
		if (value == null)
			return null;
		
		AttributeValueSyntax<?> attributeSyntax = getAttributeSyntaxSafe(attributeName);
		
		if (attributeSyntax != null && attributeSyntax.isEmailVerifiable())
		{
			return (VerifiableElementBase) attributeSyntax.convertFromString(value);
		}else
		{
			return new VerifiableElementBase(value);
		}
	}

	@Transactional
	private AttributeValueSyntax<?> getAttributeSyntaxSafe(String attributeName)
	{
		try
		{
			return atHelper.getUnconfiguredSyntaxForAttributeName(attributeName);
		} catch (Exception e)
		{
			// ok
			log.debug("Can not get attribute syntax for attribute " + attributeName);
			return null;
		}
	}

	public String getAttributeFromMeta(long entityId, String path, String metadata) throws EngineException
	{
		VerifiableElementBase verValue = getVerifiableAttributeFromMeta(entityId, path, metadata);
		if (verValue == null)
			return null;
		return verValue.getValue();
	}
	
	public VerifiableElementBase getVerifiableAttributeFromMeta(long entityId, String path, String metadata) throws EngineException
	{
		AttributeType attrType = attrHelper.getAttributeTypeWithSingeltonMetadata(metadata);
		if (attrType == null)
			return null;

		Optional<String> value = getAttributeValue(entityId, path, attrType.getName());

		if (!value.isPresent())
			return null;

		return getVerifiableAttributeValue(attrType.getName(), value.get());
	}

}
