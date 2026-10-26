package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.store.api.GroupDAO;

@ExtendWith(MockitoExtension.class)
class ProjectOperationValidatorTest
{
	private static final String PROJECT = "/project";

	@Mock
	private GroupDAO groups;

	@ParameterizedTest
	@MethodSource("validationCases")
	void shouldEnforceOperationConstraints(ValidationCase testCase)
	{
		when(groups.get(anyString())).thenAnswer(invocation ->
		{
			String path = invocation.getArgument(0);
			Group group = new Group(path);
			group.setDelegationConfiguration(new GroupDelegationConfiguration(
					path.equals(PROJECT) ? testCase.delegation() : testCase.targetDelegation(),
					testCase.subprojects(), null, null, null, null, List.of(), List.of()));
			return group;
		});
		ProjectOperationValidator validator = new ProjectOperationValidator(groups);

		Throwable failure = catchThrowable(() ->
		{
			switch (testCase.check())
			{
			case PROJECT -> validator.validateProject(PROJECT);
			case GROUP -> validator.validateGroup(PROJECT, testCase.group());
			case SUBPROJECT -> validator.validateSubproject(PROJECT, testCase.group());
			case ROLE -> validator.validateRoleAssignment(PROJECT, testCase.group());
			}
		});

		if (testCase.valid())
			assertThat(failure).isNull();
		else
			assertThat(failure).isNotNull().hasMessageContaining(testCase.error());
	}

	@Test
	void shouldReportGroupLookupFailure()
	{
		IllegalStateException cause = new IllegalStateException("storage unavailable");
		when(groups.get(PROJECT)).thenThrow(cause);

		assertThatThrownBy(() -> new ProjectOperationValidator(groups).validateProject(PROJECT))
				.isInstanceOf(InternalException.class).hasMessageContaining(PROJECT).hasCause(cause);
	}

	private static Stream<ValidationCase> validationCases()
	{
		return Stream.of(
				Arrays.stream(Check.values()).map(check -> ValidationCase.builder(check).build()),
				Arrays.stream(Check.values()).map(check -> ValidationCase.builder(check)
						.withDelegation(false).withError("enabled delegation").build()),
				Stream.of(Check.GROUP, Check.SUBPROJECT, Check.ROLE).flatMap(check ->
						Stream.of(PROJECT, PROJECT + "/a/b").map(path -> ValidationCase.builder(check)
								.withGroup(path).build())),
				Stream.of(Check.GROUP, Check.SUBPROJECT, Check.ROLE).flatMap(check ->
						Stream.of("/other", PROJECT + "-other").map(path -> ValidationCase.builder(check)
								.withGroup(path).withError("not child of main project").build())),
				Stream.of(Check.PROJECT, Check.GROUP, Check.ROLE).map(check -> ValidationCase.builder(check)
						.withSubprojects(false).build()),
				Stream.of(ValidationCase.builder(Check.SUBPROJECT).withSubprojects(false)
						.withError("subprojects creation").build(),
						ValidationCase.builder(Check.ROLE).withTargetDelegation(false)
								.withError("enabled delegation on /project/sub").build(),
						ValidationCase.builder(Check.GROUP).withTargetDelegation(false).build()))
				.flatMap(stream -> stream);
	}

	private enum Check
	{
		PROJECT, GROUP, SUBPROJECT, ROLE
	}

	private record ValidationCase(Check check, String group, boolean delegation, boolean subprojects,
			boolean targetDelegation, String error)
	{
		boolean valid()
		{
			return error == null;
		}

		static Builder builder(Check check)
		{
			return new Builder(check);
		}

		private static class Builder
		{
			private final Check check;
			private String group = PROJECT + "/sub";
			private boolean delegation = true;
			private boolean subprojects = true;
			private boolean targetDelegation = true;
			private String error;

			private Builder(Check check)
			{
				this.check = check;
			}

			Builder withGroup(String group)
			{
				this.group = group;
				return this;
			}

			Builder withDelegation(boolean delegation)
			{
				this.delegation = delegation;
				return this;
			}

			Builder withSubprojects(boolean subprojects)
			{
				this.subprojects = subprojects;
				return this;
			}

			Builder withTargetDelegation(boolean targetDelegation)
			{
				this.targetDelegation = targetDelegation;
				return this;
			}

			Builder withError(String error)
			{
				this.error = error;
				return this;
			}

			ValidationCase build()
			{
				return new ValidationCase(check, group, delegation, subprojects, targetDelegation, error);
			}
		}
	}
}
