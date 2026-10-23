package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;

@Tag("home")
class AccountRemovalTest extends HomeWorkflowTestBase
{
	@Test
	void shouldScheduleRemovalAndCancelItThroughFreshLoginDuringGracePeriod() throws Exception
	{
		TestUser user = createUser("Scheduled removal");
		TestUser control = createUser("Removal control");
		deployHome("");
		VaadinPage page = openProfile(user);
		page.click(testId("EntityRemovalButton.remove"));
		page.fill(testId("ScheduledEntityRemovalDialog.days"), "7");
		Instant earliest = Instant.now().plus(7, ChronoUnit.DAYS);
		page.click(testId("ScheduledEntityRemovalDialog.submit"));
		confirm(page, true);

		EntityInformation scheduled = entities.getEntity(user.entity()).getEntityInformation();
		assertThat(scheduled.getState()).isEqualTo(EntityState.onlyLoginPermitted);
		assertThat(scheduled.getRemovalByUserTime().toInstant())
				.isBetween(earliest, Instant.now().plus(7, ChronoUnit.DAYS));
		assertAccepted(user);
		EntityInformation reactivated = entities.getEntity(user.entity()).getEntityInformation();
		assertThat(reactivated.getState()).isEqualTo(EntityState.valid);
		assertThat(reactivated.getRemovalByUserTime()).isNull();
		assertAccepted(control);
		assertThat(entities.getEntity(control.entity()).getState()).isEqualTo(EntityState.valid);
	}

	@ParameterizedTest
	@MethodSource("invalidSchedules")
	void shouldRejectInvalidGracePeriodAndLeaveAccountUnchangedOnConfirmationCancellation(ScheduleCase scenario)
			throws Exception
	{
		TestUser user = createUser("Invalid removal schedule");
		deployHome("");
		VaadinPage page = openProfile(user);
		page.click(testId("EntityRemovalButton.remove"));
		page.fill(testId("ScheduledEntityRemovalDialog.days"), scenario.input());
		page.click(testId("ScheduledEntityRemovalDialog.submit"));
		page.visible(By.cssSelector("[data-testid='ScheduledEntityRemovalDialog.days'][invalid]"));
		assertThat(driver.findElements(testId("ScheduledEntityRemovalDialog.confirmation")))
				.noneMatch(element -> element.isDisplayed());
		assertUnscheduled(user);

		page.fill(testId("ScheduledEntityRemovalDialog.days"), "7");
		page.click(testId("ScheduledEntityRemovalDialog.submit"));
		confirm(page, false);
		waitForElementNotPresent(testId("ScheduledEntityRemovalDialog"));
		assertUnscheduled(user);
		assertAccepted(user);
	}

	@ParameterizedTest
	@MethodSource("removalModes")
	void shouldApplyConfiguredImmediateRemovalAndPreserveAnotherAccount(RemovalCase scenario) throws Exception
	{
		TestUser user = createUser("Immediate removal");
		TestUser control = createUser("Immediate control");
		deployHome("unity.userhome.disableSelfRemovalScheduling=true\nunity.userhome.selfRemovalMode="
				+ scenario.mode() + "\n");
		VaadinPage page = openProfile(user);
		page.click(testId("EntityRemovalButton.remove"));
		page.visible(testId("ImmediateEntityRemovalDialog"));
		driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
		waitForElementNotPresent(testId("ImmediateEntityRemovalDialog"));
		assertUnscheduled(user);
		page.click(testId("EntityRemovalButton.remove"));
		page.click(testId("ImmediateEntityRemovalDialog", "DialogWithActionFooter.confirm"));
		waitForUi();
		if (scenario.expected() == null)
			assertRemoved(user);
		else
			assertThat(entities.getEntity(user.entity()).getState()).isEqualTo(scenario.expected());
		assertRejected(user);
		assertThat(entities.getEntity(control.entity()).getState()).isEqualTo(EntityState.valid);
		assertAccepted(control);
	}

	@Test
	void shouldRemoveImmediatelyWhenSelectedInSchedulingDialog() throws Exception
	{
		TestUser user = createUser("Remove now");
		deployHome("");
		VaadinPage page = openProfile(user);
		page.click(testId("EntityRemovalButton.remove"));
		page.visible(testId("ScheduledEntityRemovalDialog.mode")).findElements(By.tagName("vaadin-radio-button"))
				.stream().filter(option -> option.getText().equals("Remove immediately"))
				.findFirst().orElseThrow().click();
		page.click(testId("ScheduledEntityRemovalDialog.submit"));
		confirm(page, true);
		assertRemoved(user);
		assertRejected(user);
	}

	private void confirm(VaadinPage page, boolean confirm)
	{
		page.click(By.cssSelector("[data-testid='ScheduledEntityRemovalDialog.confirmation'] [slot='"
				+ (confirm ? "confirm" : "cancel") + "-button']"));
		waitForElementNotPresent(testId("ScheduledEntityRemovalDialog.confirmation"));
		waitForUi();
	}

	private void assertUnscheduled(TestUser user) throws Exception
	{
		EntityInformation state = entities.getEntity(user.entity()).getEntityInformation();
		assertThat(state.getState()).isEqualTo(EntityState.valid);
		assertThat(state.getRemovalByUserTime()).isNull();
	}

	private void assertRemoved(TestUser user)
	{
		assertThatThrownBy(() -> entities.getEntity(user.entity())).isInstanceOf(UnknownIdentityException.class);
		forgetUser(user.entity());
	}

	private static Stream<ScheduleCase> invalidSchedules()
	{
		return Stream.of(ScheduleCase.builder().input("0").build(), ScheduleCase.builder().input("366").build(),
				ScheduleCase.builder().input("not-a-number").build());
	}

	private static Stream<RemovalCase> removalModes()
	{
		return Stream.of(RemovalCase.builder().mode("remove").build(),
				RemovalCase.builder().mode("disable").expected(EntityState.disabled).build(),
				RemovalCase.builder().mode("blockAuthentication").expected(EntityState.authenticationDisabled).build());
	}

	record ScheduleCase(String input)
	{
		static Builder builder()
		{
			return new Builder();
		}

		static final class Builder
		{
			private String input;

			Builder input(String input)
			{
				this.input = input;
				return this;
			}

			ScheduleCase build()
			{
				return new ScheduleCase(input);
			}
		}
	}

	record RemovalCase(String mode, EntityState expected)
	{
		static Builder builder()
		{
			return new Builder();
		}

		static final class Builder
		{
			private String mode;
			private EntityState expected;

			Builder mode(String mode)
			{
				this.mode = mode;
				return this;
			}

			Builder expected(EntityState expected)
			{
				this.expected = expected;
				return this;
			}

			RemovalCase build()
			{
				return new RemovalCase(mode, expected);
			}
		}
	}
}
