/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.ldaputils.LDAPAttributeType;
import pl.edu.icm.unity.ldaputils.LDAPAttributeTypeConverter;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

/**
 * Provides conversion of well known LDAP attribute types. The converter is quite flexible:
 * it supports (nearly all) LDAP attributes for which there are syntax mappings.
 * 
 * @author K. Benedyczak
 */
@Component
public class StandardLDAPConverter implements LDAPAttributeTypeConverter
{
	@Override
	public boolean supports(LDAPAttributeType at)
	{
		AttributeValueSyntax<?> syntax = getSyntaxForOid(at.getSyntax());
		return syntax != null;
	}

	@Override
	public List<AttributeType> convertSingle(LDAPAttributeType at)
	{
		AttributeValueSyntax<?> syntax = getSyntaxForOid(at.getSyntax());
		List<AttributeType> ret = new ArrayList<AttributeType>(at.getNames().size());
		for (String name: at.getNames())
		{
			AttributeType converted = new AttributeType(name, syntax);
			if (at.isSingleValue())
			{
				converted.setMaxElements(1);
				converted.setMinElements(1);
			} else
			{
				converted.setMinElements(0);
				converted.setMaxElements(256);
			}
			if (at.getDescription() != null)
				converted.setDescription(at.getDescription());
			ret.add(converted);
		}
		return ret;
	}

	
	private AttributeValueSyntax<?> getSyntaxForOid(String oid)
	{
		if (oid == null)
			return null;
		int size = -1;
		if (oid.contains("{"))
		{
			String[] split = oid.split("\\{");
			oid = split[0];
			String val = split[1].substring(0, split[1].indexOf('}'));
			size = Integer.parseInt(val);
		}
		
		if (oid.equals("1.3.6.1.4.1.1466.115.121.1.11")) // country string
		{
			StringAttributeSyntax ret = new StringAttributeSyntax();
			try
			{
				ret.setMaxLength(2);
				ret.setMinLength(2);
			} catch (WrongArgumentException e) 
			{
				throw new java.lang.IllegalArgumentException(e);
			}
			return ret;
		}
		
		if (oid.equals("1.3.6.1.4.1.1466.115.121.1.15") || // Directory string
				oid.equals("1.3.6.1.4.1.1466.115.121.1.26") || //IA5 String
				oid.equals("1.3.6.1.4.1.1466.115.121.1.44"))  //Printable string
				
		{
			StringAttributeSyntax ret = new StringAttributeSyntax();
			if (size != -1)
				try
				{
					ret.setMaxLength(size);
				} catch (WrongArgumentException e)
				{
					throw new java.lang.IllegalArgumentException(e);
				}
			return ret;
		}
		
		if (oid.equals("1.3.6.1.4.1.1466.115.121.1.27")) // Integer
		{
			return new IntegerAttributeSyntax();
		}

		if (oid.equals("1.3.6.1.4.1.1466.115.121.1.28")) // Jpeg
		{
			return new JpegImageAttributeSyntax();
		}

		if (oid.equals("1.3.6.1.4.1.1466.115.121.1.36")) //String number
		{
			return new IntegerAttributeSyntax();
		}
		
		if (oid.equals("1.3.6.1.4.1.1466.115.121.1.44")) //Printable string
		{
			return new StringAttributeSyntax();
		}
		return null;
	}
}
