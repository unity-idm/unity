/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.attributes.PublicAttributeInfo;
import pl.edu.icm.unity.store.api.AttributeDAO;

/**
 * For a given attribute, which syntax indicates that is sharable, link the
 * external id keyword with an attribute. The purpose is to have reliable and
 * fast way of searching attributes by external id.
 */
class PublicAttributeRegistry
{
	private final AttributeDAO attributeDAO;
	private final AttributeTypeHelper atHelper;

	@Autowired
	PublicAttributeRegistry(AttributeDAO attributeDAO, AttributeTypeHelper atHelper)
	{
		this.attributeDAO = attributeDAO;
		this.atHelper = atHelper;
	}

	void registerAttributeInfo(Attribute attr, long createdAttrId)
	{
		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(attr.getValueSyntax());
		syntax.publicExposureSpec().ifPresent(spec ->
		{
			attr.getValues().forEach(stringRepresentation ->
			{
				PublicAttributeInfo info = spec.getInfo(stringRepresentation);
				attributeDAO.linkKeywordToAttribute(info.externalId, createdAttrId);
			});
		});
	}
}
