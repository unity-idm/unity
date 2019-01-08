/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.utils.MobileNumberUtils;
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;

/**
 * Verifiable mobile number attribute value syntax.
 * @author P. Piernik
 */
public class VerifiableMobileNumberAttributeSyntax implements AttributeValueSyntax<VerifiableMobileNumber> 
{
	public static final String ID = "verifiableMobileNumber";
	private MobileNumberConfirmationConfiguration mobileNumberConfirmationConfiguration;

	
	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public boolean areEqual(VerifiableMobileNumber value, Object another)
	{
		return value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public void validate(VerifiableMobileNumber value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		
		String error = MobileNumberUtils.validate(value.getValue());
		if (error != null)
			throw new IllegalAttributeValueException(value.getValue() + ": " + error);
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		if (getMobileNumberConfirmationConfiguration().isPresent())
		{
			main.set("mobileConfirmationConfiguration", getMobileNumberConfirmationConfiguration().get().toJson());
		}
		return main;
	}
	
	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
		if (JsonUtil.notNull(json, "mobileConfirmationConfiguration")) 
			setMobileNumberConfirmationConfiguration(new MobileNumberConfirmationConfiguration((ObjectNode) json.get("mobileConfirmationConfiguration")));		
	}

	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}

	@Override
	public boolean isUserVerifiable()
	{
		return true;
	}

	@Override
	public VerifiableMobileNumber convertFromString(String stringRepresentation)
	{
		return new VerifiableMobileNumber(JsonUtil.parse(stringRepresentation));
	}

	@Override
	public String convertToString(VerifiableMobileNumber value)
	{
		return JsonUtil.serialize(value.toJson());
	}

	@Override
	public String serializeSimple(VerifiableMobileNumber value)
	{
		return value.getValue();
	}

	@Override
	public VerifiableMobileNumber deserializeSimple(String value) throws IllegalAttributeValueException
	{
		VerifiableMobileNumber ret = MobileNumberUtils.convertFromString(value);
		validate(ret);
		return ret;
	}
	
	public void setMobileNumberConfirmationConfiguration(
			MobileNumberConfirmationConfiguration confirmationConfiguration)
	{
		this.mobileNumberConfirmationConfiguration = confirmationConfiguration;
	}
	
	public Optional<MobileNumberConfirmationConfiguration> getMobileNumberConfirmationConfiguration()
	{
		return Optional.ofNullable(mobileNumberConfirmationConfiguration);
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<VerifiableMobileNumber>
	{
		public Factory()
		{
			super(VerifiableMobileNumberAttributeSyntax.ID, VerifiableMobileNumberAttributeSyntax::new);
		}
	}
}
