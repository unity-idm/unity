/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.nimbusds.jose.util.Base64;

import net.minidev.json.JSONArray;
import pl.edu.icm.unity.stdext.attr.BooleanAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Default OAuth attribute mapper, maps string, string arrays, numbers, enums and images.
 * 
 * @author K. Benedyczak
 */
public class DefaultOAuthAttributeMapper implements OAuthAttributeMapper
{
	private static final Map<String, ValueToJsonConverter> VALUE_TO_OAUTH = 
			new HashMap<String, ValueToJsonConverter>();
	
	static 
	{
		ValueToJsonConverter[] converters = new ValueToJsonConverter[] {
				new SimpleValueConverter(),
				new EmailValueConverter(),
				new JpegValueConverter(),
				new BooleanValueConverter()
		};

		for (ValueToJsonConverter conv: converters)
		{
			for (String syntax: conv.getSupportedSyntaxes())
				VALUE_TO_OAUTH.put(syntax, conv);
		}
	}

	
	@Override
	public boolean isHandled(Attribute unityAttribute)
	{
		String syntax = unityAttribute.getValueSyntax();
		return VALUE_TO_OAUTH.containsKey(syntax);
	}


	@Override
	public Object getJsonValue(Attribute unityAttribute)
	{
		int valsNum = unityAttribute.getValues().size(); 
		String syntax = unityAttribute.getValueSyntax();
		ValueToJsonConverter converter = VALUE_TO_OAUTH.get(syntax);
		
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
	public String getJsonKey(Attribute unityAttribute)
	{
		return unityAttribute.getName();
	}

	private static class SimpleValueConverter implements ValueToJsonConverter
	{
		@Override
		public Object convertValueToJson(String value)
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
		private static final JpegImageAttributeSyntax syntax = 
				new JpegImageAttributeSyntax();

		@Override
		public String convertValueToJson(String value)
		{
			BufferedImage decoded = syntax.convertFromString(value);
			byte[] octets = new JpegImageAttributeSyntax().serialize(decoded);
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
		private static final VerifiableEmailAttributeSyntax syntax = 
				new VerifiableEmailAttributeSyntax();
		
		@Override
		public String convertValueToJson(String value)
		{
			VerifiableEmail parsed = syntax.convertFromString(value);
			return parsed.getValue();
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {VerifiableEmailAttributeSyntax.ID};
		}
	}

	private static class BooleanValueConverter implements ValueToJsonConverter
	{
		private static final BooleanAttributeSyntax syntax = 
				new BooleanAttributeSyntax();
		
		@Override
		public Boolean convertValueToJson(String value)
		{
			return syntax.convertFromString(value);
		}

		@Override
		public String[] getSupportedSyntaxes()
		{
			return new String[] {BooleanAttributeSyntax.ID};
		}
	}

	
	private interface ValueToJsonConverter
	{
		Object convertValueToJson(String value);
		String[] getSupportedSyntaxes();
	}
}
