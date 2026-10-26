/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.imunity.rest.api.RestAttributeCacheValidationReport;
import io.imunity.rest.api.RestAttributeMismatch;
import io.imunity.rest.api.RestGroupValidationReport;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.attributes.EffectiveAttributesCacheService;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class TestAttributeCacheValidation extends RESTAdminTestBase
{
	private static final String PATH = "/restadm/v1/attribute-cache-validation";

	@Autowired
	private EffectiveAttributesCacheService cacheService;
	@Autowired
	private AttributesCacheDAO attributesCacheDAO;
	@Autowired
	private TransactionalRunner tx;

	private long entityId;

	@BeforeEach
	public void setupData() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		entityId = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "e1"), EntityState.valid)
				.getEntityId();
		groupsMan.addMemberFromParent("/A", new EntityParam(entityId));
		attrsMan.createAttribute(new EntityParam(entityId), StringAttribute.of("a1", "/A", "v1"));
		awaitCacheRefreshed();
	}

	@Test
	public void reportsAllGroupsWhenGroupOmitted() throws Exception
	{
		RestAttributeCacheValidationReport report = parse(executeQuery(new HttpGet(PATH)));

		assertThat(report.groups).extracting(g -> g.group).contains("/", "/A");
	}

	@Test
	public void reportsMismatchOfSelectedGroupWithCompleteStructure() throws Exception
	{
		tx.runInTransaction(() -> attributesCacheDAO.deleteCacheInGroup(entityId, "/A"));

		RestAttributeCacheValidationReport report = parse(executeQuery(new HttpGet(PATH + "?group=%2FA")));

		assertThat(report.groups).hasSize(1);
		RestGroupValidationReport group = report.groups.get(0);
		assertThat(group.group).isEqualTo("/A");
		assertThat(group.entitiesWithPendingUpdate).isEmpty();
		assertThat(group.entitiesWithMismatches).hasSize(1);
		assertThat(group.entitiesWithMismatches.get(0).entityId).isEqualTo(entityId);
		RestAttributeMismatch mismatch = group.entitiesWithMismatches.get(0).mismatches.get(0);
		assertThat(mismatch.attributeName).isEqualTo("a1");
		assertThat(mismatch.cachedValues).isEmpty();
		assertThat(mismatch.freshValues).containsExactly("v1");
		assertThat(mismatch.valuesRedacted).isFalse();
	}

	@Test
	public void acceptsGroupWithoutLeadingSlash() throws Exception
	{
		RestAttributeCacheValidationReport report = parse(executeQuery(new HttpGet(PATH + "?group=A")));

		assertThat(report.groups).extracting(g -> g.group).containsExactly("/A");
	}

	@Test
	public void nonexistentGroupIsMappedToBadRequest() throws Exception
	{
		try (ClassicHttpResponse response = client.executeOpen(host, new HttpGet(PATH + "?group=%2Fmissing"),
				getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(400);
			assertThat(EntityUtils.toString(response.getEntity())).contains("/missing");
		}
	}

	@Test
	public void userWithoutMaintenanceCapabilityIsForbidden() throws Exception
	{
		createUsernameUser("inspector", "Inspector", DEF_PASSWORD, CRED_REQ_PASS);
		HttpClientContext inspector = getClientContext(host, "inspector", DEF_PASSWORD);

		try (ClassicHttpResponse response = client.executeOpen(host, new HttpGet(PATH), inspector))
		{
			assertThat(response.getCode()).isEqualTo(403);
		}
	}

	private void awaitCacheRefreshed() throws InterruptedException
	{
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
		while (cacheService.isUpdatePending(entityId, "/A"))
		{
			assertThat(System.currentTimeMillis()).as("cache refreshed in time").isLessThan(deadline);
			Thread.sleep(50);
		}
	}

	private RestAttributeCacheValidationReport parse(String json) throws Exception
	{
		return m.readValue(json, RestAttributeCacheValidationReport.class);
	}
}
