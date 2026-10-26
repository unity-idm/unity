/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Security sensitive attributes (credentials) must never get into the attributes cache, nor become searchable.
 */
public class AttributesCacheSensitiveAttributesTest extends AttributesCacheIntegrationTestBase
{
	@Autowired
	private AttributeSearchService searchService;
	@Autowired
	private pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationService validationService;

	@Test
	public void credentialAttributesAreNotCachedNorSearchable() throws Exception
	{
		setupPasswordAuthn();
		Identity user = createUsernameUser("withPassword", null, DEF_PASSWORD, CRED_REQ_PASS);
		long entityId = user.getEntityId();
		String credentialValue = insecureAttrsMan.getAllAttributes(new pl.edu.icm.unity.base.entity.EntityParam(
				entityId), false, "/", CredentialAttributeTypeProvider.CREDENTIAL_PREFIX + "credential1", false)
				.iterator().next().getValues().get(0);
		startBackgroundRefresh();
		awaitFreshCachedValues(entityId, "/", CredentialAttributeTypeProvider.CREDENTIAL_PREFIX + "credential1",
				null);

		List<StoredAttribute> stored = tx.runInTransactionRet(
				() -> attributesCacheDAO.getEntityAttributes(entityId, "/"));
		assertThat(stored).extracting(sa -> sa.getAttribute().getName())
				.noneMatch(name -> name.startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX));
		Map<String, AttributeExt> consistent = effectiveAttributesCache.getAttributes(entityId, "/");
		assertThat(consistent.keySet())
				.noneMatch(name -> name.startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX));
		String fragment = credentialValue.substring(credentialValue.length() / 2,
				credentialValue.length() / 2 + 10);
		assertThat(searchService.searchEntities("/", fragment)).doesNotContain(entityId);
	}

	@Test
	public void liveComputedReadsOfPendingEntityOmitCredentials() throws Exception
	{
		setupPasswordAuthn();
		long entityId = createUsernameUser("withPassword", null, DEF_PASSWORD, CRED_REQ_PASS).getEntityId();
		assertThat(isPending(entityId, "/")).isTrue();

		assertThat(effectiveAttributesCache.getAttributes(entityId, "/").keySet())
				.noneMatch(name -> name.startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX));
		assertThat(effectiveAttributesCache.getGroupAttributes("/").get(entityId).keySet())
				.noneMatch(name -> name.startsWith(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX));
	}

	@Test
	public void validationReportsNoMismatchForOmittedCredentials() throws Exception
	{
		setupPasswordAuthn();
		long entityId = createUsernameUser("withPassword", null, DEF_PASSWORD, CRED_REQ_PASS).getEntityId();
		startBackgroundRefresh();
		awaitFreshCachedValues(entityId, "/", CredentialAttributeTypeProvider.CREDENTIAL_PREFIX + "credential1",
				null);

		assertThat(validationService.validate(java.util.Optional.of("/")).groups().get(0)
				.entitiesWithMismatches()).noneMatch(e -> e.entityId() == entityId);
	}
}
