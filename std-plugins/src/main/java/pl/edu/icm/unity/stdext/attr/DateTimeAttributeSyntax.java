/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * DateTime attribute sytax. Accept datetime in various formats
 * 
 * @author P.Piernik
 *
 */
public class DateTimeAttributeSyntax implements AttributeValueSyntax<LocalDateTime>
{
	public static final String ID = "datetime";
	public static List<String> acceptableFormats = 
			pl.edu.icm.unity.stdext.attr.DateAttributeSyntax.acceptableFormats.stream()
			.map(f -> f + "['T'][ ]HH:mm:ss").collect(Collectors.toList());

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		return Constants.MAPPER.createObjectNode();
	}

	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
		// OK
	}

	@Override
	public void validate(LocalDateTime value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");

	}

	@Override
	public boolean areEqual(LocalDateTime value, Object another)
	{
		return value == null ? null == another : value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public LocalDateTime convertFromString(String stringRepresentation)
	{
		for (String format : acceptableFormats)
		{
			try
			{
				LocalDateTime date = LocalDateTime.parse(stringRepresentation,
						DateTimeFormatter.ofPattern(format));

				return date;

			} catch (Exception e)
			{
				// OK
			}
		}

		throw new InternalException("Can not parse datetime " + stringRepresentation
				+ " using standart date formats");
	}

	@Override
	public String convertToString(LocalDateTime value)
	{
		return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	@Override
	public boolean isVerifiable()
	{
		return false;
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<LocalDateTime>
	{
		public Factory()
		{
			super(DateTimeAttributeSyntax.ID, DateTimeAttributeSyntax::new);
		}
	}

}
