/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.verifiable.VerifiableElement;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;


@Component
class ExternalDataParserImpl implements ExternalDataParser
{
	private final AttributeValueConverter attributeValueConverter;
	private final IdentityTypesRegistry idsRegistry;
	private final AttributeTypeSupport attributeTypeSupport;
	private final AttributeTypeHelper atHelper;
	
	@Autowired
	ExternalDataParserImpl(AttributeValueConverter attributeValueConverter,
			IdentityTypesRegistry idsRegistry, AttributeTypeSupport attributeTypeSupport,
			AttributeTypeHelper atHelper)
	{
		this.attributeValueConverter = attributeValueConverter;
		this.idsRegistry = idsRegistry;
		this.attributeTypeSupport = attributeTypeSupport;
		this.atHelper = atHelper;
	}

	@Override
	public Attribute parseAsAttribute(String unityAttributeName, String group, List<?> externalValues) 
			throws IllegalAttributeValueException
	{
		AttributeType unityAttribute = attributeTypeSupport.getType(unityAttributeName);
		return parseAsAttribute(unityAttribute, group, externalValues, null, null);
	}

	@Override
	public Attribute parseAsAttribute(AttributeType unityAttribute, String group, List<?> externalValues,
			String idp, String profile) throws IllegalAttributeValueException
	{
		List<String> typedValues = attributeValueConverter.externalValuesToInternal(unityAttribute.getName(), 
				externalValues);
		return new Attribute(unityAttribute.getName(), unityAttribute.getValueSyntax(), group, 
				typedValues, idp, profile);
	}

	@Override
	public <T> Attribute parseAsConfirmedAttribute(AttributeType unityAttribute, String group, List<?> externalValues,
			String idp, String profile) throws IllegalAttributeValueException
	{
		@SuppressWarnings("unchecked")
		AttributeValueSyntax<T> syntax = (AttributeValueSyntax<T>) atHelper.getSyntaxForAttributeName(
				unityAttribute.getName());
		List<String> internalValues = attributeValueConverter.externalValuesToInternal(syntax, externalValues);

		List<T> typedValues = attributeValueConverter.internalValuesToObjectValues(syntax, internalValues);
		for (Object val: typedValues)
		{
			if (val instanceof VerifiableElement)
				((VerifiableElement) val).setConfirmationInfo(new ConfirmationInfo(true));
		}
		List<String> internalConfirmedValues = attributeValueConverter.objectValuesToInternalValues(syntax, typedValues);
		return new Attribute(unityAttribute.getName(), unityAttribute.getValueSyntax(), group, 
				internalConfirmedValues, idp, profile);
	}
	
	@Override
	public IdentityParam parseAsIdentity(String identityType, Object externalValue) throws IllegalIdentityValueException
	{
		return parseAsIdentity(idsRegistry.getByName(identityType), externalValue, null, null);
	}

	@Override
	public IdentityParam parseAsIdentity(IdentityTypeDefinition identityTypeDefinition, Object externalValue, String idp,
			String profile) throws IllegalIdentityValueException
	{
		return identityTypeDefinition.convertFromString(externalValue.toString(), idp, profile);
	}

	@Override
	public IdentityParam parseAsConfirmedIdentity(IdentityTypeDefinition identityType, Object externalValue,
			String idp, String profile) throws IllegalIdentityValueException
	{
		IdentityParam parsed = parseAsIdentity(identityType, externalValue, idp, profile);
		if (identityType.isEmailVerifiable())
			parsed.setConfirmationInfo(new ConfirmationInfo(true));
		return parsed;
	}
}
