package io.imunity.test.ui.upman;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

@Tag("public")
class InvitationLinkTest extends ProjectInvitationTestBase
{
	@Autowired
	@Qualifier("insecure")
	private InvitationManagement invitations;

	@ParameterizedTest(name = "{0}")
	@MethodSource("unusableLinks")
	void shouldRejectUnusableInvitationWithoutAnotherRequestOrMembershipChange(LinkState scenario) throws Exception
	{
		Project project = prepareProject();
		TestUser manager = createUser("Invitation error manager");
		join(project, manager, GroupAuthorizationRole.manager);
		openMembers(manager);
		VaadinPage page = new VaadinPage(driver);
		invite(page, RECIPIENT);
		String link = notificationTo(RECIPIENT).body().strip();
		InvitationWithCode invitation = invitations.getInvitations().getFirst();
		if (scenario.state().equals("invalid"))
			link = link.replace(invitation.getRegistrationCode(), "invalid-invitation-code");
		else if (scenario.state().equals("expired"))
			invitations.updateInvitation(invitation.getRegistrationCode(), new ComboInvitationParam.Builder(
					(ComboInvitationParam) invitation.getInvitation())
						.withExpiration(Instant.now().minusSeconds(1)).build());
		else
		{
			var submitted = submitInvitedRegistration(newBrowser(), link);
			decide(page, submitted.getRequestId(), true);
			waitFor(() -> registrationRequests().getFirst().getStatus() == RegistrationRequestStatus.accepted);
			trackUser(new EntityParam(registrations.getRegistrationRequest(
						submitted.getRequestId()).getCreatedEntityId()));
		}
		var before = members(project);
		int requestsBefore = registrationRequests().size();
		var browser = newBrowser();
		browser.get(link);
		new VaadinPage(browser).visible(By.className("u-final-error"));
		assertThat(browser.findElements(VaadinPage.testId("StandaloneRegistration.submit"))).isEmpty();
		assertThat(registrationRequests()).hasSize(requestsBefore);
		assertThat(members(project)).containsExactlyInAnyOrderElementsOf(before);
		screenshot(browser, "Unusable project invitation");
	}

	private static Stream<LinkState> unusableLinks()
	{
		return Stream.of(LinkState.builder().state("invalid").build(),
				LinkState.builder().state("expired").build(), LinkState.builder().state("used").build());
	}

	private record LinkState(String state)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private String state;

			Builder state(String value)
			{
				state = value;
				return this;
			}

			LinkState build()
			{
				return new LinkState(state);
			}
		}
	}
}
