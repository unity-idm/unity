/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.LocalDateTime;
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
 * DateTime attribute sytax. Accept datetime in various formats
 * 
 * @author P.Piernik
 *
 */
public class DateTimeAttributeSyntax implements AttributeValueSyntax<LocalDateTime>
{
	public static final String ID = "datetime";
	public static List<String> ACCEPTABLE_FORMATS = Arrays.asList(
			"yyyy-MM-dd['T'][ ]HH:mm:ss", "dd-MM-yyyy['T'][ ]HH-mm-ss",
			"ddMMyy['T'][ ]HHmmss", "dd.MM.yyyy['T'][ ]HH.mm.ss",
			"ddMMyyyy['T'][ ]HHmmss", "dd/MM/yyyy['T'][ ]HH/mm/ss");
	
	private static final Logger log = Log.getLogger(Log.U_SERVER, DateTimeAttributeSyntax.class);	
	
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
		for (String format : ACCEPTABLE_FORMATS)
		{
			try
			{
				return LocalDateTime.parse(stringRepresentation,
						DateTimeFormatter.ofPattern(format));

			} catch (Exception e)
			{
				log.trace("Can not parse datetime " + stringRepresentation
						+ " using format: " + format, e);
			}
		}

		throw new InternalException("Can not parse datetime " + stringRepresentation
				+ " using standard date formats");
	}

	@Override
	public String convertToString(LocalDateTime value)
	{
		return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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
	public static class Factory extends AbstractAttributeValueSyntaxFactory<LocalDateTime>
	{
		public Factory()
		{
			super(DateTimeAttributeSyntax.ID, DateTimeAttributeSyntax::new);
		}
	}

}
