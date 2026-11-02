package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.RestProjectAuthorization;
import pl.edu.icm.unity.oauth.api.Scope;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.SystemOAuthScopeProvidersRegistry;

class UpmanRestOAuthAuthorizationTest
{
	@Test
	void shouldRegisterGlobalAndPatternProjectScopes()
	{
		List<Scope> scopes = new UpmanSystemScopeProvider().getScopes();

		assertThat(scopes).hasSize(2);
		assertThat(scopes.get(0).name).isEqualTo(UpmanSystemScopeProvider.API_SCOPE);
		assertThat(scopes.get(0).pattern).isFalse();
		assertThat(scopes.get(1).pattern).isTrue();
		assertThat(Pattern.matches(scopes.get(1).name, "sys:upman-api:project-x")).isTrue();
		OAuthScopesService scopeService = new OAuthScopesService(
				new SystemOAuthScopeProvidersRegistry(Optional.of(List.of(new UpmanSystemScopeProvider()))));
		assertThat(scopeService.getSystemScopes()).anySatisfy(scope ->
		{
			assertThat(scope.name).isEqualTo(UpmanSystemScopeProvider.PROJECT_SCOPE_PATTERN);
			assertThat(scope.pattern).isTrue();
		});
	}

	@Test
	void shouldReturnOnlyProjectsCoveredByToken() throws Exception
	{
		RestProjectAuthorization authorization = mock(RestProjectAuthorization.class);
		when(authorization.isProjectVisible("/A/project-x")).thenReturn(true);
		GroupsManagement groups = mock(GroupsManagement.class);
		when(groups.getGroupsByWildcard("/A/**"))
				.thenReturn(List.of(project("project-x"), project("project-y")));
		RestProjectService service = new RestProjectService(mock(DelegatedGroupManagement.class), groups,
				authorization, mock(EntityManagement.class), mock(RegistrationsManagement.class),
				mock(EnquiryManagement.class), "/A");

		assertThat(service.getProjects()).extracting(project -> project.projectId)
				.containsExactly("project-x");
	}

	@Test
	void shouldPassCanonicalNestedProjectPathToEveryRestService() throws Exception
	{
		assertCanonicalAuthorization("parent/child");
	}

	@Test
	void shouldDecodeEscapedLiteralProjectIdBeforeAuthorization() throws Exception
	{
		assertCanonicalAuthorization("parent/team%west");
	}

	private void assertCanonicalAuthorization(String projectId) throws Exception
	{
		RestProjectAuthorization authorization = mock(RestProjectAuthorization.class);
		String path = "/A/" + projectId;
		String urlId = URLDecoder.decode(ProjectPathProvider.urlId(projectId), StandardCharsets.UTF_8);
		doThrow(new AuthorizationException("denied")).when(authorization).assertManagerAuthorization(path);
		RestProjectService projects = new RestProjectService(mock(DelegatedGroupManagement.class),
				mock(GroupsManagement.class), authorization, mock(EntityManagement.class),
				mock(RegistrationsManagement.class), mock(EnquiryManagement.class), "/A");
		RestUpmanWorkflowService workflows = new RestUpmanWorkflowService();
		workflows.init(null, null, null, null, authorization, "/A");
		RestProjectFormService forms = new RestProjectFormService(authorization, null, "/A");
		RestProjectPolicyDocumentService policies = new RestProjectPolicyDocumentService(authorization,
				null, null, null, null, "/A");

		assertThatThrownBy(() -> projects.getProject(urlId)).isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> workflows.groups(urlId)).isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> forms.getRegistrationForm(urlId)).isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policies.getPolicyDocuments(urlId)).isInstanceOf(AuthorizationException.class);
		verify(authorization, times(4)).assertManagerAuthorization(path);
	}

	private Group project(String id)
	{
		Group project = new Group("/A/" + id);
		project.setDelegationConfiguration(new GroupDelegationConfiguration(true, false,
				null, null, null, null, List.of(), List.of()));
		return project;
	}
}
