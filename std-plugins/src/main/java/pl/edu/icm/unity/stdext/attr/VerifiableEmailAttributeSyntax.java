/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
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
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

/**
 * Verifiable email attribute value syntax.
 * @author P. Piernik
 */
public class VerifiableEmailAttributeSyntax implements AttributeValueSyntax<VerifiableEmail> 
{
	public static final String ID = "verifiableEmail";
	private EmailConfirmationConfiguration emailConfirmationConfiguration;

	
	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public boolean areEqual(VerifiableEmail value, Object another)
	{
		return value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public void validate(VerifiableEmail value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		String error = EmailUtils.validate(value.getValue());
		if (error != null)
			throw new IllegalAttributeValueException(value.getValue() + ": " + error);
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		if (getEmailConfirmationConfiguration().isPresent())
		{
			main.set("emailConfirmationConfiguration", getEmailConfirmationConfiguration().get().toJson());
		}
		return main;
	}
	
	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
		if (JsonUtil.notNull(json, "emailConfirmationConfiguration")) 
			setEmailConfirmationConfiguration(new EmailConfirmationConfiguration((ObjectNode) json.get("emailConfirmationConfiguration")));		
	}

	@Override
	public boolean isEmailVerifiable()
	{
		return true;
	}

	@Override
	public boolean isUserVerifiable()
	{
		return true;
	}

	@Override
	public VerifiableEmail convertFromString(String stringRepresentation)
	{
		return new VerifiableEmail(JsonUtil.parse(stringRepresentation));
	}

	@Override
	public String convertToString(VerifiableEmail value)
	{
		return JsonUtil.serialize(value.toJson());
	}

	@Override
	public String serializeSimple(VerifiableEmail value)
	{
		return value.getValue();
	}

	@Override
	public VerifiableEmail deserializeSimple(String value) throws IllegalAttributeValueException
	{
		VerifiableEmail ret = EmailUtils.convertFromString(value);
		validate(ret);
		return ret;
	}
	
	public void setEmailConfirmationConfiguration(
			EmailConfirmationConfiguration confirmationConfiguration)
	{
		this.emailConfirmationConfiguration = confirmationConfiguration;
	}
	
	public Optional<EmailConfirmationConfiguration> getEmailConfirmationConfiguration()
	{
		return Optional.ofNullable(emailConfirmationConfiguration);
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<VerifiableEmail>
	{
		public Factory()
		{
			super(VerifiableEmailAttributeSyntax.ID, VerifiableEmailAttributeSyntax::new);
		}
	}
}
