/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldaputils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Loads attribute types from LDAP schema files and converts them to {@link AttributeType} instances.
 * @author K. Benedyczak
 */
@Component
public class LDAPAttributeTypesConverter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, LDAPAttributeTypesConverter.class);
	private static List<LDAPAttributeType> STANDARD_TYPES; 
	
	static 
	{
		InputStream is = LDAPAttributeTypesConverter.class.getClassLoader().getResourceAsStream(
				"pl/edu/icm/unity/server/core/ldapschema/core.schema");
		try
		{
			STANDARD_TYPES = LDAPAttributeTypesLoader.loadWithInheritance(new InputStreamReader(is), null);
		} catch (Exception e)
		{
			STANDARD_TYPES = Collections.emptyList();
			log.error("Can not load system LDAP attribute types", e);
		}
	}
			
	private List<LDAPAttributeTypeConverter> converters;
	
	@Autowired
	public LDAPAttributeTypesConverter(List<LDAPAttributeTypeConverter> converters)
	{
		this.converters = converters;
	}

	public List<AttributeType> convert(Reader input) throws RecognitionException, TokenStreamException
	{
		List<LDAPAttributeType> ldapTypes = LDAPAttributeTypesLoader.loadWithInheritance(input, STANDARD_TYPES);
		
		List<AttributeType> ret = new ArrayList<AttributeType>();
		for (LDAPAttributeType at: ldapTypes)
		{
			try
			{
				for (LDAPAttributeTypeConverter converter:converters)
					if (converter.supports(at))
					{
						List<AttributeType> converted = converter.convertSingle(at);
						ret.addAll(converted);
						break;
					}
			} catch (Exception e)
			{
				log.warn("Converter thrown an exception", e);
			}
			
		}
		
		return ret;
	}
}
