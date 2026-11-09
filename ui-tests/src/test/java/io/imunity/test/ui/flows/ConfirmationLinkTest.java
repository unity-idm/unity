package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailAttribiuteConfirmationState;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;

@Tag("public")
class ConfirmationLinkTest extends EmailWorkflowTestBase
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("unusableLinks")
	void shouldRejectUnusableConfirmationWithoutChangingSavedEmail(LinkCase scenario) throws Exception
	{
		TestUser user = emailOwner();
		attributes.setAttribute(user.entity(), VerifiableEmailAttribute.of(EMAIL_ATTRIBUTE, "/", NEW_EMAIL));
		confirmations.sendConfirmationRequest(new EmailAttribiuteConfirmationState(user.entity().getEntityId(),
				EMAIL_ATTRIBUTE, NEW_EMAIL, "en", "/"));
		String link = confirmationLink();
		VaadinPage page = new VaadinPage(driver);
		switch (scenario.state())
		{
			case INVALID -> link = link.replace(tokenFrom(link), "not-a-confirmation-token");
			case EXPIRED -> tokens.updateToken(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE, tokenFrom(link),
					Date.from(Instant.now().minusSeconds(60)), null);
			case CONSUMED ->
			{
				driver.get(link);
				assertThat(page.visible(By.className("u-final-info")).getText()).contains("Email confirmed");
				assertThat(emails(user)).allMatch(email -> email.getConfirmationInfo().isConfirmed());
			}
		}

		driver.get(link);
		assertThat(page.visible(By.className("u-final-error")).getText()).contains("token");
		assertThat(emails(user)).singleElement().satisfies(email ->
		{
			assertThat(email.getValue()).isEqualTo(NEW_EMAIL);
			assertThat(email.getConfirmationInfo().isConfirmed()).isEqualTo(scenario.state() == State.CONSUMED);
		});
	}

	private static Stream<LinkCase> unusableLinks()
	{
		return Stream.of(LinkCase.builder().state(State.INVALID).build(),
				LinkCase.builder().state(State.EXPIRED).build(), LinkCase.builder().state(State.CONSUMED).build());
	}

	private enum State
	{
		INVALID, EXPIRED, CONSUMED
	}

	private record LinkCase(State state)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private State state;

			Builder state(State value)
			{
				state = value;
				return this;
			}

			LinkCase build()
			{
				return new LinkCase(state);
			}
		}
	}
}
