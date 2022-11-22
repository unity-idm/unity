/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
import pl.edu.icm.unity.engine.api.idp.UserImportConfigs;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.successful;

public class IdPEngineImplBaseTest
{
	@Test
	public void shouldExposeImportStatusToProfile_EarlyImport() throws EngineException
	{
		AttributesManagement attributesMan = mock(AttributesManagement.class);
		GroupsManagement groupMan = mock(GroupsManagement.class);
		EntityManagement identitiesMan = mock(EntityManagement.class);
		UserImportSerivce userImportService = mock(UserImportSerivce.class);
		OutputProfileExecutor outputProfileExecutor = mock(OutputProfileExecutor.class);
		UserImportConfigs config = new UserImportConfigs(false, Set.of());
		
		when(userImportService.importUser(any())).thenReturn(
				Lists.newArrayList(
					new UserImportSerivce.ImportResult("imp1", successful(
							mock(RemotelyAuthenticatedPrincipal.class), 
							mock(AuthenticatedEntity.class)))));
		
		IdPEngineImplBase tested = new IdPEngineImplBase(attributesMan, attributesMan,
				identitiesMan, userImportService, outputProfileExecutor, groupMan);
		
		
		tested.obtainUserInformationWithEarlyImport(
				new IdentityTaV("idType", "id"), 
				"/group", 
				TranslationProfileGenerator.generateIncludeOutputProfile("profile"), 
				"requester",
				Optional.empty(),
				"protocol", 
				"protocolSubType", 
				false, 
				config);
		
		ArgumentCaptor<TranslationInput> captor = ArgumentCaptor.forClass(TranslationInput.class);
		verify(outputProfileExecutor).execute(eq(TranslationProfileGenerator.generateIncludeOutputProfile("profile")), captor.capture());
		TranslationInput ti = captor.getValue();
		assertThat(ti.getImportStatus().size(), is(1));
		assertThat(ti.getImportStatus().get("imp1"), is(notNullValue()));
		assertThat(ti.getImportStatus().get("imp1"), is(Status.success));
	}
	
	@Test
	public void shouldExposeImportStatusToProfile_LateImport() throws EngineException
	{
		AttributesManagement attributesMan = mock(AttributesManagement.class);
		GroupsManagement groupMan = mock(GroupsManagement.class);
		EntityManagement identitiesMan = mock(EntityManagement.class);
		UserImportSerivce userImportService = mock(UserImportSerivce.class);
		OutputProfileExecutor outputProfileExecutor = mock(OutputProfileExecutor.class);
		UserImportConfigs config = new UserImportConfigs(false, Set.of());
		
		when(identitiesMan.getEntity(any(), any(), eq(false), any())).thenReturn(
				new Entity(Lists.newArrayList(new Identity("idType", "id", 1, "id")), null, null));
		when(userImportService.importToExistingUser(any(), any())).thenReturn(
				Lists.newArrayList(
					new UserImportSerivce.ImportResult("imp1", successful(
							mock(RemotelyAuthenticatedPrincipal.class), 
							mock(AuthenticatedEntity.class)))));
		
		IdPEngineImplBase tested = new IdPEngineImplBase(attributesMan, attributesMan,
				identitiesMan, userImportService, outputProfileExecutor, groupMan);
		
		tested.obtainUserInformationWithEnrichingImport(
				new EntityParam(1l), 
				"/group", 
				TranslationProfileGenerator.generateIncludeOutputProfile("profile"), 
				"requester",
				Optional.empty(),
				"protocol", 
				"protocolSubType", 
				false, 
				config);
		
		ArgumentCaptor<TranslationInput> captor = ArgumentCaptor.forClass(TranslationInput.class);
		verify(outputProfileExecutor).execute(eq(TranslationProfileGenerator.generateIncludeOutputProfile("profile")), captor.capture());
		TranslationInput ti = captor.getValue();
		assertThat(ti.getImportStatus().size(), is(1));
		assertThat(ti.getImportStatus().get("imp1"), is(notNullValue()));
		assertThat(ti.getImportStatus().get("imp1"), is(Status.success));
	}
	
	@Test
	public void shouldExposeRequesterAttributesToProfile_LateImport() throws EngineException
	{
		AttributesManagement attributesMan = mock(AttributesManagement.class);
		GroupsManagement groupMan = mock(GroupsManagement.class);
		AttributesManagement insecureAttributesMan = mock(AttributesManagement.class);
		EntityManagement identitiesMan = mock(EntityManagement.class);
		UserImportSerivce userImportService = mock(UserImportSerivce.class);
		OutputProfileExecutor outputProfileExecutor = mock(OutputProfileExecutor.class);
		UserImportConfigs config = new UserImportConfigs(false, Set.of());
		
		EntityParam clientEntity = new EntityParam(1234l);
		List<AttributeExt> clientAttributes = Lists.newArrayList(new AttributeExt(
				StringAttribute.of("attr", "/GROUP", Lists.newArrayList("v1")), true));
		when(identitiesMan.getEntity(any(), any(), eq(false), any())).thenReturn(
				new Entity(Lists.newArrayList(new Identity("idType", "id", 1, "id")), null, null));
		when(userImportService.importToExistingUser(any(), any())).thenReturn(
				Lists.newArrayList(
					new UserImportSerivce.ImportResult("imp1", 
							successful(mock(RemotelyAuthenticatedPrincipal.class), 
									mock(AuthenticatedEntity.class)))));
		
		when(insecureAttributesMan.getAttributes(eq(clientEntity), eq("/GROUP"), eq(null)))
				.thenReturn(clientAttributes);
		
		
		IdPEngineImplBase tested = new IdPEngineImplBase(attributesMan, 
				insecureAttributesMan, identitiesMan, 
				userImportService, outputProfileExecutor, groupMan);
		
		tested.obtainUserInformationWithEnrichingImport(
				new EntityParam(1l), 
				"/group", 
				TranslationProfileGenerator.generateIncludeOutputProfile("profile"), 
				"requester",
				Optional.of(new EntityInGroup("/GROUP", clientEntity)),
				"protocol", 
				"protocolSubType", 
				false, 
				config);
		
		ArgumentCaptor<TranslationInput> captor = ArgumentCaptor.forClass(TranslationInput.class);
		verify(outputProfileExecutor).execute(eq(TranslationProfileGenerator.generateIncludeOutputProfile("profile")), captor.capture());
		TranslationInput ti = captor.getValue();
		assertThat(ti.getRequesterAttributes().size(), is(1));
		assertThat(ti.getRequesterAttributes(), is(clientAttributes));
	}
}
