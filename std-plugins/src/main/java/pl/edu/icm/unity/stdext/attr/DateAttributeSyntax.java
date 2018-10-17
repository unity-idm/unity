/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Date attribute sytax. Accept date in various formats
 * 
 * @author P.Piernik
 *
 */
public class DateAttributeSyntax implements AttributeValueSyntax<LocalDate>
{
	public static final String ID = "date";
	public static List<String> ACCEPTABLE_FORMATS = Arrays.asList("yyyy-MM-dd", "dd-MM-yyyy",
			"ddMMyy", "dd.MM.yyyy", "ddMMyyyy", "dd/MM/yyyy");
	
	private static final Logger log = Log.getLogger(Log.U_SERVER, DateAttributeSyntax.class);	
	
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
	public void validate(LocalDate value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");

	}

	@Override
	public boolean areEqual(LocalDate value, Object another)
	{
		return value == null ? null == another : value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public LocalDate convertFromString(String stringRepresentation)
	{
		for (String format : ACCEPTABLE_FORMATS)
		{
			try
			{
				return LocalDate.parse(stringRepresentation,
						DateTimeFormatter.ofPattern(format));

			} catch (Exception e)
			{
				log.trace("Can not parse date " + stringRepresentation
						+ " using format: " + format, e);
			}
		}

		throw new InternalException("Can not parse date " + stringRepresentation
				+ " using standard date formats");
	}

	@Override
	public String convertToString(LocalDate value)
	{
		return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}

	@Override
	public boolean isUserVerifiable()
	{
		return false;
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<LocalDate>
	{
		public Factory()
		{
			super(DateAttributeSyntax.ID, DateAttributeSyntax::new);
		}
	}

}
