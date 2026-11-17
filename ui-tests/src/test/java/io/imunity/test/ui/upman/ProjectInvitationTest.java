package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

@Tag("upman")
class ProjectInvitationTest extends ProjectInvitationTestBase
{
	@Autowired
	@Qualifier("insecure")
	private InvitationManagement invitations;

	@ParameterizedTest(name = "{0}")
	@MethodSource("decisions")
	void shouldApplyManagerDecisionToInvitedRegistration(Decision scenario) throws Exception
	{
		Project project = prepareProject();
		TestUser manager = createUser("Invitation manager");
		join(project, manager, GroupAuthorizationRole.manager);
		openMembers(manager);
		VaadinPage page = new VaadinPage(driver);
		invite(page, RECIPIENT);
		String link = notificationTo(RECIPIENT).body().strip();
		RegistrationRequestState submitted = submitInvitedRegistration(newBrowser(), link);
		assertThat(submitted.getStatus()).isEqualTo(RegistrationRequestStatus.pending);
		assertThat(submitted.getCreatedEntityId()).isNull();
		assertThat(members(project)).containsExactly(manager.entity().getEntityId());

		decide(page, submitted.getRequestId(), scenario.accept());
		waitFor(() -> registrationRequests().getFirst().getStatus() != RegistrationRequestStatus.pending);
		RegistrationRequestState decided = registrations.getRegistrationRequest(submitted.getRequestId());
		if (scenario.accept())
		{
			assertThat(decided.getStatus()).isEqualTo(RegistrationRequestStatus.accepted);
			EntityParam created = new EntityParam(decided.getCreatedEntityId());
			trackUser(created);
			assertThat(members(project))
				.containsExactlyInAnyOrder(manager.entity().getEntityId(), created.getEntityId());
			VaadinPage login = new VaadinPage(newBrowser());
			login.login(baseUrl, "/home", username, PASSWORD);
			assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains("Invited project member");
		} else
		{
			assertThat(decided.getStatus()).isEqualTo(RegistrationRequestStatus.rejected);
			assertThat(decided.getCreatedEntityId()).isNull();
			assertThat(members(project)).containsExactly(manager.entity().getEntityId());
		}
		driver.navigate().refresh();
		page.visible(testId("UserUpdatesView.requests"));
		assertThat(driver.findElements(By.cssSelector("[data-testid='UserUpdatesView.actions']"
				+ "[data-request-id='" + submitted.getRequestId() + "']"))).isEmpty();
	}

	@Test
	void shouldInviteExistingUserThroughEnquiryWithoutCreatingAnotherAccount() throws Exception
	{
		Project project = prepareProject();
		TestUser manager = createUser("Invitation manager");
		TestUser invitee = createUser("Existing invitation recipient");
		IdentityParam email = new IdentityParam("email", RECIPIENT);
		email.setConfirmationInfo(new ConfirmationInfo(true));
		entities.addIdentity(email, invitee.entity());
		join(project, manager, GroupAuthorizationRole.manager);
		openMembers(manager);
		VaadinPage page = new VaadinPage(driver);
		invite(page, RECIPIENT);
		var recipient = newBrowser();
		recipient.get(notificationTo(RECIPIENT).body().strip());
		waitFor(() -> recipient.getWindowHandles().size() == 2);
		String initialWindow = recipient.getWindowHandle();
		recipient.switchTo().window(recipient.getWindowHandles().stream()
				.filter(window -> !window.equals(initialWindow)).findFirst().orElseThrow());
		VaadinPage enquiry = new VaadinPage(recipient);
		screenshot(recipient, "Existing user invitation enquiry");
		enquiry.click(testId("StandaloneEnquiryView.submit"));
		enquiry.visible(By.className("u-final-info"));
		assertThat(registrationRequests()).isEmpty();
		assertThat(enquiries.getEnquiryResponses()).singleElement();
		var request = enquiries.getEnquiryResponses().getFirst();
		assertThat(members(project)).containsExactly(manager.entity().getEntityId());
		decide(page, request.getRequestId(), true);
		waitFor(() -> driver.findElements(By.cssSelector("[data-testid='UserUpdatesView.actions']"
				+ "[data-request-id='" + request.getRequestId() + "']")).stream()
				.noneMatch(element -> element.isDisplayed()));
		assertThat(members(project)).containsExactlyInAnyOrder(manager.entity().getEntityId(),
				invitee.entity().getEntityId());
		assertThat(entities.getEntity(new EntityParam(new IdentityTaV("email", RECIPIENT)))
				.getId()).isEqualTo(invitee.entity().getEntityId());
		assertThat(registrationRequests()).isEmpty();
	}

	@Test
	void shouldFilterResendAndRevokeInvitationAndInvalidateItsLink() throws Exception
	{
		Project project = prepareProject();
		TestUser manager = createUser("Invitation manager");
		join(project, manager, GroupAuthorizationRole.manager);
		openMembers(manager);
		VaadinPage page = new VaadinPage(driver);
		invite(page, RECIPIENT);
		String link = notificationTo(RECIPIENT).body().strip();
		invite(page, "unrelated@example.org");
		InvitationWithCode invitation = invitations.getInvitations().stream()
				.filter(item -> RECIPIENT.equals(item.getInvitation().getContactAddress())).findFirst().orElseThrow();
		page.fill(testId("InvitationsView.search"), RECIPIENT);
		waitFor(() -> page.visible(testId("InvitationsView.invitations")).getText().contains(RECIPIENT)
				&& !page.visible(testId("InvitationsView.invitations")).getText().contains("unrelated@example.org"));
		By actions = By.cssSelector("[data-testid='InvitationsView.actions'][data-invitation-code='"
				+ invitation.getRegistrationCode() + "']");
		screenshot("Filtered project invitation");
		int sentBefore = notifications.sentTo(RECIPIENT).size();
		page.click(actions);
		page.click(testId("InvitationsView.resend"));
		waitFor(() -> notifications.sentTo(RECIPIENT).size() > sentBefore);
		assertThat(notificationTo(RECIPIENT).body().strip()).isEqualTo(link);
		page.click(actions);
		page.click(testId("InvitationsView.revoke"));
		waitFor(() -> driver.findElements(actions).stream().noneMatch(element -> element.isDisplayed()));
		assertThat(invitations.getInvitations()).extracting(item -> item.getInvitation().getContactAddress())
				.containsExactly("unrelated@example.org");
		driver.navigate().refresh();
		assertThat(page.visible(testId("InvitationsView.invitations")).getText()).doesNotContain(RECIPIENT);

		var recipient = newBrowser();
		recipient.get(link);
		new VaadinPage(recipient).visible(By.className("u-final-error"));
		assertThat(registrationRequests()).isEmpty();
		assertThat(members(project)).containsExactly(manager.entity().getEntityId());
	}

	private static Stream<Decision> decisions()
	{
		return Stream.of(Decision.builder().accept(true).build(), Decision.builder().accept(false).build());
	}

	private record Decision(boolean accept)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private boolean accept;

			Builder accept(boolean value)
			{
				accept = value;
				return this;
			}

			Decision build()
			{
				return new Decision(accept);
			}
		}
	}
}
