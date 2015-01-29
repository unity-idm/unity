/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import net.minidev.json.JSONArray;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;

import com.nimbusds.jose.util.Base64;

/**
 * Default OAuth attribute mapper, maps string, string arrays, numbers, enums and images.
 * 
 * @author K. Benedyczak
 */
public class DefaultOAuthAttributeMapper implements OAuthAttributeMapper
{
	private static final Map<String, ValueToJsonConverter> VALUE_TO_SAML = 
			new HashMap<String, ValueToJsonConverter>();
	
	static 
	{
		ValueToJsonConverter[] converters = new ValueToJsonConverter[] {
				new SimpleValueConverter(),
				new EmailValueConverter(),
				new JpegValueConverter()
		};

		for (ValueToJsonConverter conv: converters)
		{
			for (String syntax: conv.getSupportedSyntaxes())
				VALUE_TO_SAML.put(syntax, conv);
		}
	}

	
	@Override
	public boolean isHandled(Attribute<?> unityAttribute)
	{
		String syntax = unityAttribute.getAttributeSyntax().getValueSyntaxId();
		return VALUE_TO_SAML.containsKey(syntax);
	}


	@Override
	public Object getJsonValue(Attribute<?> unityAttribute)
	{
		int valsNum = unityAttribute.getValues().size(); 
		String syntax = unityAttribute.getAttributeSyntax().getValueSyntaxId();
		ValueToJsonConverter converter = VALUE_TO_SAML.get(syntax);
		
		if (valsNum > 1)
		{
			JSONArray array = new JSONArray();
			for (int i=0; i<valsNum; i++)
			{
				array.add(converter.convertValueToJson(unityAttribute.getValues().get(i)));
			}
			return array;
		} else if (valsNum == 1)
		{
			return converter.convertValueToJson(unityAttribute.getValues().get(0));
		} else
		{
			return null;
		}
	}

	@Override
	public String getJsonKey(Attribute<?> unityAttribute)
	{
		return unityAttribute.getName();
	}

	private static class SimpleValueConverter implements ValueToJsonConverter
	{
		@Override
		public Object convertValueToJson(Object value)
		{
			return value;
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {FloatingPointAttributeSyntax.ID,
					IntegerAttributeSyntax.ID, 
					StringAttributeSyntax.ID, 
					EnumAttributeSyntax.ID};
		}
	}

	private static class JpegValueConverter implements ValueToJsonConverter
	{
		@Override
		public String convertValueToJson(Object value)
		{
			byte[] octets = new JpegImageAttributeSyntax().serialize((BufferedImage) value);
			return Base64.encode(octets).toJSONString();
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {JpegImageAttributeSyntax.ID};
		}
	}

	private static class EmailValueConverter implements ValueToJsonConverter
	{
		@Override
		public String convertValueToJson(Object value)
		{
			return ((VerifiableEmail) value).getValue();
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {VerifiableEmailAttributeSyntax.ID};
		}
	}
	
	private interface ValueToJsonConverter
	{
		Object convertValueToJson(Object value);
		String[] getSupportedSyntaxes();
	}
}
