/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;

public class ExternalDataParserTest extends DBIntegrationTestBase
{
	@Autowired
	private ExternalDataParser parser;
	
	@Test
	public void shouldParseAsPlainAttribute() throws EngineException
	{
		AttributeType oType = new AttributeType("attr", VerifiableEmailAttributeSyntax.ID);
		aTypeMan.addAttributeType(oType);
		
		Attribute parsed = parser.parseAsAttribute(oType.getName(), "/", newArrayList("some@example.com"));
		
		assertThat(parsed).isEqualTo(VerifiableEmailAttribute.of(oType.getName(), "/", "some@example.com"));
	}

	@Test
	public void shouldParseAsConfirmedAttribute() throws EngineException
	{
		AttributeType oType = new AttributeType("attr", VerifiableEmailAttributeSyntax.ID);
		aTypeMan.addAttributeType(oType);
		
		Attribute parsed = parser.parseAsConfirmedAttribute(oType, "/", newArrayList("some@example.com"), "idp", "tp");


		VerifiableEmail parsedValue = new VerifiableEmail(JsonUtil.parse(parsed.getValues().get(0)));
		long confirmTS = parsedValue.getConfirmationInfo().getConfirmationDate();
		assertThat(confirmTS).isGreaterThan(0);
		ConfirmationInfo expectedConfirmation = new ConfirmationInfo(true);
		expectedConfirmation.setConfirmationDate(confirmTS);
		Attribute expected = VerifiableEmailAttribute.of(oType.getName(), "/", 
				new VerifiableEmail("some@example.com", expectedConfirmation));
		expected.setRemoteIdp("idp");
		expected.setTranslationProfile("tp");
		assertThat(parsed).isEqualTo(expected);
	}

	@Test
	public void shouldParseAsPlainIdentity() throws IllegalIdentityValueException
	{
		IdentityParam parsed = parser.parseAsIdentity(EmailIdentity.ID, "some@example.com");
		
		EmailIdentity emialId = new EmailIdentity();
		assertThat(parsed).isEqualTo(emialId.convertFromString("some@example.com", null, null));
	}

	@Test
	public void shouldParseAsConfirmedIdentity() throws IllegalIdentityValueException
	{
		EmailIdentity emailId = new EmailIdentity();
		IdentityParam parsed = parser.parseAsConfirmedIdentity(emailId, "some@example.com", "idp", "tp");

		
		IdentityParam expected = emailId.convertFromString("some@example.com", "idp", "tp");
		long confirmTS = parsed.getConfirmationInfo().getConfirmationDate();
		assertThat(confirmTS).isGreaterThan(0);
		ConfirmationInfo expectedConfirmation = new ConfirmationInfo(true);
		expectedConfirmation.setConfirmationDate(confirmTS);
		expected.setConfirmationInfo(expectedConfirmation);
		assertThat(parsed).isEqualTo(expected);
	}
}
