package io.imunity.test.ui.flows;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;

public abstract class EmailWorkflowTestBase extends EndUserTestBase
{
	protected static final String EMAIL_ATTRIBUTE = "ui-confirmation-email";
	protected static final String ORIGINAL_EMAIL = "original@example.org";
	protected static final String NEW_EMAIL = "new@example.org";

	@Autowired
	protected EmailConfirmationManager confirmations;
	@Autowired
	protected TokensManagement tokens;
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;

	protected final TestUser emailOwner() throws Exception
	{
		messageTemplate("ui-confirm-email", "EmailConfirmation", "${confirmationLink}", "default_email");
		VerifiableEmailAttributeSyntax syntax = new VerifiableEmailAttributeSyntax();
		syntax.setEmailConfirmationConfiguration(new EmailConfirmationConfiguration("ui-confirm-email"));
		AttributeType type = new AttributeType(EMAIL_ATTRIBUTE, VerifiableEmailAttributeSyntax.ID);
		type.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		type.setMinElements(1);
		type.setMaxElements(2);
		type.setSelfModificable(true);
		types.addAttributeType(type);
		TestUser user = createUser("Email owner");
		attributes.setAttribute(user.entity(), VerifiableEmailAttribute.of(EMAIL_ATTRIBUTE, "/",
				new VerifiableEmail(ORIGINAL_EMAIL, new ConfirmationInfo(true))));
		return user;
	}

	protected final List<VerifiableEmail> emails(TestUser user) throws Exception
	{
		return attributes.getAttributes(user.entity(), "/", EMAIL_ATTRIBUTE).iterator().next().getValues().stream()
				.map(value -> VerifiableEmail.fromJsonString(value.toString())).toList();
	}

	protected final String confirmationLink()
	{
		return notificationTo(NEW_EMAIL).body().strip();
	}

	protected final String tokenFrom(String link)
	{
		return Arrays.stream(URI.create(link).getRawQuery().split("&"))
				.filter(parameter -> parameter.startsWith("token="))
				.map(parameter -> URLDecoder.decode(parameter.substring(6), StandardCharsets.UTF_8))
				.findFirst().orElseThrow();
	}
}
