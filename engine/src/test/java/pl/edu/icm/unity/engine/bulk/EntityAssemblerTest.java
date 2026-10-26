/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@ExtendWith(MockitoExtension.class)
public class EntityAssemblerTest
{
	private static final long ENTITY = 1L;

	@Mock
	private EntityCredentialsHelper credentialsHelper;
	@Mock
	private LocalCredentialsRegistry localCredReg;

	private EntitiesData entitiesData;
	private GlobalSystemData globalData;
	private EntityInformation entityInfo;
	private Identity plainIdentity;
	private Identity targetedIdentity;
	private CredentialInfo credentialInfo;

	@BeforeEach
	public void setup()
	{
		entityInfo = new EntityInformation(ENTITY);
		entityInfo.setEntityState(EntityState.disabled);
		plainIdentity = new Identity("identifier", "plain", ENTITY, "plain");
		targetedIdentity = new Identity("targetedPersistent", "targeted", ENTITY, "targeted");
		targetedIdentity.setTarget("some-sp");
		Map<String, AttributeExt> rootAttributes = Map.of("cr",
				new AttributeExt(StringAttribute.of("cr", "/", "credReq1"), true));
		entitiesData = EntitiesData.builder()
				.withEntityInfo(Map.of(ENTITY, entityInfo))
				.withIdentities(Map.of(ENTITY, List.of(plainIdentity, targetedIdentity)))
				.withDirectAttributes(Map.of(ENTITY, Map.of("/", rootAttributes)))
				.withMemberships(Map.of())
				.build();
		globalData = GlobalSystemData.builder()
				.withGroups(Map.of())
				.withAttributeTypes(Map.of())
				.withAttributeClasses(Map.of())
				.withEnquiryForms(Map.of())
				.withCredentialRequirements(Map.of("credReq1", new CredentialRequirements("credReq1", "", Set.of())))
				.withCredentials(List.of())
				.build();
		credentialInfo = new CredentialInfo("credReq1", Map.of());
		when(credentialsHelper.getCredentialReqFromAttribute(rootAttributes)).thenReturn("credReq1");
		when(credentialsHelper.getCredentialInfoNoQuery(eq(ENTITY), eq(rootAttributes), any(), eq("credReq1")))
				.thenReturn(credentialInfo);
	}

	@Test
	public void includesTargetedIdentitiesWhenRequested()
	{
		Entity entity = new EntityAssembler(credentialsHelper, localCredReg)
				.getGroupEntitiesNoContext(true, entitiesData, globalData).get(ENTITY);

		assertThat(entity.getIdentities()).containsExactly(plainIdentity, targetedIdentity);
		assertPreservedEntityDetails(entity);
	}

	@Test
	public void excludesTargetedIdentitiesWhenNotRequested()
	{
		Entity entity = new EntityAssembler(credentialsHelper, localCredReg)
				.getGroupEntitiesNoContext(false, entitiesData, globalData).get(ENTITY);

		assertThat(entity.getIdentities()).containsExactly(plainIdentity);
		assertPreservedEntityDetails(entity);
	}

	private void assertPreservedEntityDetails(Entity entity)
	{
		assertThat(entity.getEntityInformation()).isSameAs(entityInfo);
		assertThat(entity.getEntityInformation().getEntityState()).isEqualTo(EntityState.disabled);
		assertThat(entity.getCredentialInfo()).isSameAs(credentialInfo);
		assertThat(entity.getCredentialInfo().getCredentialRequirementId()).isEqualTo("credReq1");
	}
}
