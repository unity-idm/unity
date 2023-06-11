/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.attr;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;

public class PolicyAgreementAttributeSyntax extends AbstractStringAttributeSyntax

{
	public static final String ID = "policyagreement";

	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		return main;
	}

	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
	}

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(String value) throws IllegalAttributeValueException
	{

	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<String>
	{
		public Factory()
		{
			super(ID, PolicyAgreementAttributeSyntax::new);
		}
	}
}
