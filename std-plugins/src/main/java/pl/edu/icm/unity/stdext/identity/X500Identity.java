/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.helpers.JavaAndBCStyle;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * X.500 identity type definition
 * @author K. Benedyczak
 */
@Component
public class X500Identity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "x500Name";

	private Set<AttributeType> EXTRACTED = new HashSet<AttributeType>(16);
	private final Set<String> EXTRACTED_NAMES = new HashSet<String>(16);
	
	@Autowired
	public X500Identity(MessageSource msg)
	{
		EXTRACTED_NAMES.add("cn");
		EXTRACTED.add(new AttributeType("cn", StringAttributeSyntax.ID, msg));

		EXTRACTED_NAMES.add("o");
		EXTRACTED.add(new AttributeType("o", StringAttributeSyntax.ID, msg));
		
		EXTRACTED_NAMES.add("ou");
		EXTRACTED.add(new AttributeType("ou", StringAttributeSyntax.ID, msg));

		EXTRACTED_NAMES.add("c");
		EXTRACTED.add(new AttributeType("c", StringAttributeSyntax.ID, msg));

		EXTRACTED_NAMES.add("email");
		EXTRACTED.add(new AttributeType("email", StringAttributeSyntax.ID, msg));

		EXTRACTED_NAMES.add("l");
		EXTRACTED.add(new AttributeType("l", StringAttributeSyntax.ID, msg));

		EXTRACTED_NAMES.add("st");
		EXTRACTED.add(new AttributeType("st", StringAttributeSyntax.ID, msg));
		
		EXTRACTED_NAMES.add("surname");
		EXTRACTED.add(new AttributeType("surname", StringAttributeSyntax.ID, msg));
		
		EXTRACTED_NAMES.add("uid");
		EXTRACTED.add(new AttributeType("uid", StringAttributeSyntax.ID, msg));
		
		EXTRACTED_NAMES.add("dc");
		EXTRACTED.add(new AttributeType("dc", StringAttributeSyntax.ID, msg));

		EXTRACTED_NAMES.add("t");
		EXTRACTED.add(new AttributeType("t", StringAttributeSyntax.ID, msg));
		
		EXTRACTED = Collections.unmodifiableSet(EXTRACTED);
	}

	public X500Identity()
	{
		EXTRACTED_NAMES.add("cn");
		EXTRACTED.add(new AttributeType("cn", StringAttributeSyntax.ID, new I18nString("cn"), null));

		EXTRACTED_NAMES.add("o");
		EXTRACTED.add(new AttributeType("o", StringAttributeSyntax.ID, new I18nString("o"), null));
		
		EXTRACTED_NAMES.add("ou");
		EXTRACTED.add(new AttributeType("ou", StringAttributeSyntax.ID, new I18nString("ou"), null));

		EXTRACTED_NAMES.add("c");
		EXTRACTED.add(new AttributeType("c", StringAttributeSyntax.ID, new I18nString("c"), null));

		EXTRACTED_NAMES.add("email");
		EXTRACTED.add(new AttributeType("email", StringAttributeSyntax.ID, new I18nString("email"), null));

		EXTRACTED_NAMES.add("l");
		EXTRACTED.add(new AttributeType("l", StringAttributeSyntax.ID, new I18nString("l"), null));

		EXTRACTED_NAMES.add("st");
		EXTRACTED.add(new AttributeType("st", StringAttributeSyntax.ID, new I18nString("st"), null));
		
		EXTRACTED_NAMES.add("surname");
		EXTRACTED.add(new AttributeType("surname", StringAttributeSyntax.ID, new I18nString("surname"), null));
		
		EXTRACTED_NAMES.add("uid");
		EXTRACTED.add(new AttributeType("uid", StringAttributeSyntax.ID, new I18nString("uid"), null));
		
		EXTRACTED_NAMES.add("dc");
		EXTRACTED.add(new AttributeType("dc", StringAttributeSyntax.ID, new I18nString("dc"), null));

		EXTRACTED_NAMES.add("t");
		EXTRACTED.add(new AttributeType("t", StringAttributeSyntax.ID, new I18nString("t"), null));
		
		EXTRACTED = Collections.unmodifiableSet(EXTRACTED);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultDescriptionKey()
	{
		return "X500Identity.description";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return EXTRACTED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value)
	{
		try
		{
			X500NameUtils.getX500Principal(value);
		} catch (Exception e)
		{
			throw new IllegalArgumentException("DN is invalid: " + 
					e.getMessage(), e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return X500NameUtils.getComparableForm(from);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute> extractAttributes(String from, Map<String, String> toExtract)
	{
		Set<ASN1ObjectIdentifier> attributeNames = X500NameUtils.getAttributeNames(from);
		JavaAndBCStyle mapper = new JavaAndBCStyle();
		List<Attribute> ret = new ArrayList<>();
		for (ASN1ObjectIdentifier attributeNameAsn: attributeNames)
		{
			String attributeName = mapper.getLabelForOidFull(attributeNameAsn).toLowerCase();
			if (!EXTRACTED_NAMES.contains(attributeName) || !toExtract.containsKey(attributeName))
				continue;
			String extractAs = toExtract.get(attributeName);
			String[] vals = X500NameUtils.getAttributeValues(from, attributeNameAsn);
			ret.add(StringAttribute.of(extractAs, "/", vals));
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return X500NameUtils.getReadableForm(from.getValue());
	}


	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("X500Identity.description");
	}
	
	@Override
	public boolean isDynamic()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("X500Identity.name");
	}
}
