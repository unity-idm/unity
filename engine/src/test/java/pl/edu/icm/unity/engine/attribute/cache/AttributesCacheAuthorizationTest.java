/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationService;
import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.AuthorizationExceptionRT;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Real roles and invocation contexts: reading cached attributes requires both read and readHidden capabilities
 * in the given group (so a privileged inspector of /A can read /A only, while a plain inspector or a regular
 * user can't read them at all), and the validation requires the maintenance capability.
 */
public class AttributesCacheAuthorizationTest extends AttributesCacheIntegrationTestBase
{
	@Autowired
	private AttributeSearchService searchService;
	@Autowired
	private AttributeCacheValidationService validationService;

	private long target;

	@BeforeEach
	public void setup() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		target = addEntity("target", "/A", "/B");
		attrsMan.createAttribute(new EntityParam(target), StringAttribute.of("a1", "/A", "needle"));
		attrsMan.createAttribute(new EntityParam(target), StringAttribute.of("a1", "/B", "needle"));
		addUser("inspectorOfA", "/A", InternalAuthorizationManagerImpl.PRIVILEGED_INSPECTOR_ROLE);
		addUser("plainInspector", "/", InternalAuthorizationManagerImpl.INSPECTOR_ROLE);
		addUser("regular", "/", InternalAuthorizationManagerImpl.USER_ROLE);
	}

	@Test
	public void groupInspectorReadsOnlyItsGroup() throws Exception
	{
		setupUserContext("inspectorOfA", null);

		assertThat(effectiveAttributesCache.getAttributes(target, "/A")).containsKey("a1");
		assertThat(effectiveAttributesCache.getGroupAttributes("/A")).containsKey(target);
		assertThat(effectiveAttributesCache.getAttributesFast(target, "/A").updatePending()).isTrue();
		assertThat(effectiveAttributesCache.getGroupAttributesFast("/A")).containsKey(target);

		assertThatThrownBy(() -> effectiveAttributesCache.getAttributes(target, "/B"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> effectiveAttributesCache.getGroupAttributes("/B"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> effectiveAttributesCache.getAttributesFast(target, "/B"))
				.isInstanceOf(AuthorizationExceptionRT.class);
		assertThatThrownBy(() -> effectiveAttributesCache.getGroupAttributesFast("/B"))
				.isInstanceOf(AuthorizationExceptionRT.class);
		assertThatThrownBy(() -> effectiveAttributesCache.isUpdatePending(target, "/B"))
				.isInstanceOf(AuthorizationExceptionRT.class);
	}

	@Test
	public void deniedSearchExposesNoMatches() throws Exception
	{
		startBackgroundRefresh();
		awaitFreshCachedValues(target, "/B", "a1", java.util.List.of("needle"));
		awaitFreshCachedValues(target, "/A", "a1", java.util.List.of("needle"));
		setupUserContext("inspectorOfA", null);

		assertThat(searchService.searchEntities("/A", "needle")).containsExactly(target);
		assertThat(searchService.searchEntities("/A", "a1", "needle")).containsExactly(target);
		assertThatThrownBy(() -> searchService.searchEntities("/B", "needle"))
				.isInstanceOf(AuthorizationExceptionRT.class);
		assertThatThrownBy(() -> searchService.searchEntities("/B", "a1", "needle"))
				.isInstanceOf(AuthorizationExceptionRT.class);
	}

	@Test
	public void regularUserCannotReadCachedAttributesOfOthers() throws Exception
	{
		setupUserContext("regular", null);

		assertThatThrownBy(() -> effectiveAttributesCache.getAttributes(target, "/A"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> effectiveAttributesCache.getGroupAttributesFast("/"))
				.isInstanceOf(AuthorizationExceptionRT.class);
		assertThatThrownBy(() -> searchService.searchEntities("/", "needle"))
				.isInstanceOf(AuthorizationExceptionRT.class);
	}

	@Test
	public void readCapabilityAloneIsInsufficient() throws Exception
	{
		setupUserContext("plainInspector", null);

		assertThatThrownBy(() -> effectiveAttributesCache.getAttributes(target, "/A"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> effectiveAttributesCache.getAttributesFast(target, "/A"))
				.isInstanceOf(AuthorizationExceptionRT.class);
		assertThatThrownBy(() -> searchService.searchEntities("/A", "needle"))
				.isInstanceOf(AuthorizationExceptionRT.class);
	}

	@Test
	public void validationRequiresMaintenanceCapability() throws Exception
	{
		setupUserContext("inspectorOfA", null);
		assertThatThrownBy(() -> validationService.validate(Optional.of("/A")))
				.isInstanceOf(AuthorizationException.class);

		setupUserContext("admin", null);
		assertThat(validationService.validate(Optional.of("/A")).groups()).hasSize(1);
	}

	private void addUser(String username, String roleGroup, String role) throws Exception
	{
		long id = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, username),
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid).getEntityId();
		EntityParam user = new EntityParam(id);
		if (!"/".equals(roleGroup))
			groupsMan.addMemberFromParent(roleGroup, user);
		attrsMan.createAttribute(user, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, roleGroup,
				role));
	}
}
