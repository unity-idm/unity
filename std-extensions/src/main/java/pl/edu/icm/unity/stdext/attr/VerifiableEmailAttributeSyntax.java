package pl.edu.icm.unity.stdext.attr;

import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

public class VerifiableEmailAttributeSyntax implements AttributeValueSyntax<VerifiableEmail> 
{
	public static final String ID = "verifiableEmail";
	private final int MIN_LENGTH = 5;
	private final int MAX_LENGTH = 33;
	private final String EMAIL_REGEXP = "[^@]+@.+\\..+";
	private Pattern pattern = null;
	
	
	
	public VerifiableEmailAttributeSyntax()
	{
		pattern = Pattern.compile(EMAIL_REGEXP);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
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
	public byte[] serialize(VerifiableEmail value) throws InternalException
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("value",value.getValue());
		main.put("verified", value.isVerified());
		main.put("verificationDate", value.getVerificationDate());
		try
		{
			return Constants.MAPPER.writeValueAsString(main).getBytes();
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize VerifiableEmail to JSON", e);
		}
	}

	@Override
	public Object serializeSimple(VerifiableEmail value) throws InternalException
	{
		return serialize(value);
	}

	@Override
	public VerifiableEmail deserialize(byte[] raw) throws InternalException
	{
		JsonNode jsonN;
		try
		{
			jsonN = Constants.MAPPER.readTree(new String(raw));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize VerifiableEmail from JSON", e);
		}
		VerifiableEmail email = new VerifiableEmail();
		email.setValue(jsonN.get("value").asText());
		email.setVerified(jsonN.get("verified").asBoolean(false));
		email.setVerificationDate(jsonN.get("verificationDate").asLong());
		
		return email;
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(VerifiableEmail value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		if (value.getValue().length() < MIN_LENGTH)
			throw new IllegalAttributeValueException("Value length (" + value.getValue().length() 
					+ ") is too small, must be at least " + MIN_LENGTH);
		if (value.getValue().length() > MAX_LENGTH)
			throw new IllegalAttributeValueException("Value length (" + value.getValue().length() 
					+ ") is too big, must be not greater than " + MAX_LENGTH);
		if (pattern != null)
			if (!pattern.matcher(value.getValue()).matches())
				throw new IllegalAttributeValueException("Value must match the " +
						"regualr expression: " + EMAIL_REGEXP);
	}

	//TODO
	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return null;
	}


	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
	      //OK
	}

}
