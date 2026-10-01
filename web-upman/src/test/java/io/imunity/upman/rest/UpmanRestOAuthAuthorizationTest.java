package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.oauth.api.Scope;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.SystemOAuthScopeProvidersRegistry;

class UpmanRestOAuthAuthorizationTest
{
	private final AttributesManagement attributes = mock(AttributesManagement.class);
	private final UpmanRestAuthorizationManager authorization = new UpmanRestAuthorizationManager(attributes);
	private InvocationContext context;

	@BeforeEach
	void setUp()
	{
		context = new InvocationContext(null, null, List.of());
		context.setLoginSession(new LoginSession(null, null, 0, 7, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.setCurrent(context);
	}

	@AfterEach
	void tearDown()
	{
		InvocationContext.setCurrent(null);
	}

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
	void shouldAllowGlobalScopeForEveryProjectAndCreation() throws Exception
	{
		context.setScopes(List.of(UpmanSystemScopeProvider.API_SCOPE));

		authorization.assertManagerAuthorization("/A");
		authorization.assertProjectAuthorization("/A", "project-x");
		authorization.assertProjectAuthorization("/A", "project-y");
	}

	@Test
	void shouldRestrictProjectScopeToItsExactProject() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:project-x"));

		authorization.assertAnyProjectAuthorization("/A");
		authorization.assertProjectAuthorization("/A", "project-x");
		assertThatThrownBy(() -> authorization.assertProjectAuthorization("/A", "project-y"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/A"))
				.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void shouldMatchEncodedNestedProjectWithoutGrantingItsSibling() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:project-x/child"));

		String projectId = URLDecoder.decode(ProjectPathProvider.urlId("project-x/child"), StandardCharsets.UTF_8);
		authorization.assertProjectAuthorization("/A", projectId);
		assertThatThrownBy(() -> authorization.assertProjectAuthorization("/A", "project-x"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertProjectAuthorization("/A", "project-x/other"))
				.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void shouldRejectOAuthTokenWithoutUpmanScope()
	{
		context.setScopes(List.of("openid"));

		assertThatThrownBy(() -> authorization.assertProjectAuthorization("/A", "project-x"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/A"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertAnyProjectAuthorization("/A"))
				.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void shouldPreserveManagerRoleAuthorizationForDirectAuthentication() throws Exception
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		AttributeExt role = mock(AttributeExt.class);
		when(role.getValues()).thenReturn(List.of("manager"));
		when(attributes.getAttributes(any(), eq("/A"), eq("sys:ProjectManagementRESTAPIRole")))
				.thenReturn(List.of(role));

		authorization.assertManagerAuthorization("/A");
		authorization.assertProjectAuthorization("/A", "project-x");
		authorization.assertAnyProjectAuthorization("/A");
	}

	@Test
	void shouldReturnOnlyProjectsCoveredByToken() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:project-x"));
		GroupsManagement groups = mock(GroupsManagement.class);
		when(groups.getGroupsByWildcard("/A/**"))
				.thenReturn(List.of(project("project-x"), project("project-y")));
		RestProjectService service = new RestProjectService(mock(DelegatedGroupManagement.class), groups,
				authorization, mock(EntityManagement.class), mock(RegistrationsManagement.class),
				mock(EnquiryManagement.class), "/A", "/A");

		assertThat(service.getProjects()).extracting(project -> project.projectId)
				.containsExactly("project-x");
	}

	private Group project(String id)
	{
		Group project = new Group("/A/" + id);
		project.setDelegationConfiguration(new GroupDelegationConfiguration(true, false,
				null, null, null, null, List.of(), List.of()));
		return project;
	}
}
