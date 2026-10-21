package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.bulkops.BulkProcessingSupport;

@Tag("admin-automation")
class AutomationTest extends UserAccountTestBase
{
	private final Set<String> ownedConditions = new HashSet<>();

	@Autowired
	@Qualifier("insecure")
	private BulkProcessingManagement rules;
	@Autowired
	private Scheduler scheduler;

	@AfterEach
	void removeRules() throws Exception
	{
		for (ScheduledProcessingRule rule : rules.getScheduledRules())
			if (ownedConditions.contains(rule.getCondition()))
				rules.removeScheduledRule(rule.getId());
	}

	@Test
	void shouldPersistScheduledRuleEditsAndRejectInvalidSchedules() throws Exception
	{
		TestUser user = createUser("Scheduled rule target");
		String condition = matching(user);
		String editedCondition = condition + " && status == 'valid'";
		ownedConditions.add(editedCondition);
		List<ScheduledProcessingRule> originalRules = rules.getScheduledRules();
		VaadinPage page = admin();
		AutomationPage automation = new AutomationPage(driver);

		page.click(testId("AutomationView.add"));
		automation.configure("AutomationEditView", condition, "disabled");
		automation.schedule("invalid schedule");
		page.click(testId("EditViewActionLayoutFactory.save"));
		automation.assertInvalidSchedule();
		assertThat(rules.getScheduledRules()).containsExactlyInAnyOrderElementsOf(originalRules);

		String schedule = futureSchedule(0);
		automation.schedule(schedule);
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/automation");
		ScheduledProcessingRule created = ownedRule();
		assertRule(created, condition, "disabled", schedule);
		automation.assertListed(created.getId(), schedule);

		driver.get(baseUrl + "/admin/automation/edit/" + created.getId());
		automation.assertConfiguration("AutomationEditView", condition, "disabled");
		automation.assertSchedule(schedule);
		automation.configure("AutomationEditView", editedCondition, "valid");
		automation.schedule("still invalid");
		page.click(testId("EditViewActionLayoutFactory.save"));
		automation.assertInvalidSchedule();
		assertThat(ownedRule()).isEqualTo(created);

		String editedSchedule = futureSchedule(15);
		automation.schedule(editedSchedule);
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/automation");
		ScheduledProcessingRule edited = ownedRule();
		assertRule(edited, editedCondition, "valid", editedSchedule);
		automation.assertListed(edited.getId(), editedSchedule);
		driver.navigate().refresh();
		automation.assertListed(edited.getId(), editedSchedule);

		driver.get(baseUrl + "/admin/automation/edit/" + edited.getId());
		driver.navigate().refresh();
		automation.assertConfiguration("AutomationEditView", editedCondition, "valid");
		automation.assertSchedule(editedSchedule);
		assertThat(state(user)).isEqualTo(EntityState.valid);
		assertThat(rules.getScheduledRules().stream()
				.filter(rule -> !ownedConditions.contains(rule.getCondition())).toList())
				.containsExactlyInAnyOrderElementsOf(originalRules);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("executionCases")
	void shouldRunRuleOnlyOnMatchingAccounts(ExecutionCase executionCase) throws Exception
	{
		TestUser target = createUser("Automation target");
		TestUser control = createUser("Automation control");
		String condition = matching(target);
		String ruleId = executionCase.saved() ? rules.scheduleRule(new ScheduledProcessingRuleParam(condition,
				new TranslationAction("changeStatus", "disabled"), futureSchedule(0))) : null;
		List<ScheduledProcessingRule> originalRules = rules.getScheduledRules();
		VaadinPage page = admin();
		AutomationPage automation = new AutomationPage(driver);

		if (executionCase.saved())
		{
			automation.assertListed(ruleId, futureSchedule(0));
			driver.get(baseUrl + "/admin/automation/run/" + ruleId);
			automation.assertConfiguration("AutomationRunView", condition, "disabled");
		}
		else
		{
			page.click(testId("AutomationView.runAdhoc"));
			automation.configure("AutomationRunView", condition, "disabled");
		}
		assertThat(state(target)).isEqualTo(EntityState.valid);
		assertThat(state(control)).isEqualTo(EntityState.valid);
		page.click(testId("AutomationRunView.run"));
		waitForPageLoadByURL("/automation");
		waitFor(() -> state(target) == EntityState.disabled);
		waitFor(this::automationFinished);
		assertThat(state(control)).isEqualTo(EntityState.valid);
		assertThat(rules.getScheduledRules()).containsExactlyInAnyOrderElementsOf(originalRules);

		WebDriver rejectedBrowser = newBrowser();
		VaadinPage rejected = new VaadinPage(rejectedBrowser);
		rejectedBrowser.get(baseUrl + "/home");
		rejected.signIn(target.username(), target.password());
		rejected.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(rejectedBrowser.getCurrentUrl()).contains("/home/authentication");
		assertThat(rejectedBrowser.findElements(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"))).isEmpty();

		VaadinPage allowed = new VaadinPage(newBrowser());
		allowed.login(baseUrl, "/home", control.username(), control.password());
		assertThat(allowed.visible(By.id("MainHeader.loggedAs")).getText()).contains(control.displayName());
	}

	private VaadinPage admin()
	{
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin/automation", "ui-admin", "Ui-admin!42");
		page.visible(testId("AutomationView.rules"));
		return page;
	}

	private String matching(TestUser user)
	{
		String condition = "idsByType['userName'] contains '" + user.username() + "'";
		ownedConditions.add(condition);
		return condition;
	}

	private ScheduledProcessingRule ownedRule() throws EngineException
	{
		List<ScheduledProcessingRule> owned = rules.getScheduledRules().stream()
				.filter(rule -> ownedConditions.contains(rule.getCondition())).toList();
		assertThat(owned).hasSize(1);
		return owned.getFirst();
	}

	private EntityState state(TestUser user)
	{
		try
		{
			return entities.getEntity(user.entity()).getState();
		} catch (EngineException e)
		{
			throw new IllegalStateException("Could not read account state", e);
		}
	}

	private boolean automationFinished()
	{
		try
		{
			return scheduler.getCurrentlyExecutingJobs().stream().noneMatch(job -> job.getJobDetail().getKey()
					.getGroup().equals(BulkProcessingSupport.JOB_GROUP));
		} catch (SchedulerException e)
		{
			throw new IllegalStateException("Could not read automation execution state", e);
		}
	}

	private static void assertRule(ScheduledProcessingRule rule, String condition, String status, String schedule)
	{
		assertThat(rule.getCondition()).isEqualTo(condition);
		assertThat(rule.getAction().getName()).isEqualTo("changeStatus");
		assertThat(rule.getAction().getParameters()).containsExactly(status);
		assertThat(rule.getCronExpression()).isEqualTo(schedule);
	}

	private static String futureSchedule(int minute)
	{
		return "0 " + minute + " 0 1 1 ? " + Year.now().plusYears(1);
	}

	private static Stream<ExecutionCase> executionCases()
	{
		return Stream.of(ExecutionCase.builder().name("ad-hoc rule").build(),
				ExecutionCase.builder().name("saved rule").saved(true).build());
	}

	record ExecutionCase(String name, boolean saved)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return name;
		}

		static final class Builder
		{
			private String name;
			private boolean saved;

			Builder name(String name)
			{
				this.name = name;
				return this;
			}

			Builder saved(boolean saved)
			{
				this.saved = saved;
				return this;
			}

			ExecutionCase build()
			{
				return new ExecutionCase(name, saved);
			}
		}
	}
}
