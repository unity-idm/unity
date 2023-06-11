/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.identity;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.engine.api.identity.DynamicIdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.audit.AuditPublisher;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdentityHelperTest extends TestCase
{
	@Mock
	private IdentityTypesRegistry idTypesRegistry;
	@Mock
	private EntityDAO entityDAO;
	@Mock
	private IdentityDAO identityDAO;
	@Mock
	private AttributeDAO attributeDAO;
	@Mock
	private IdentityTypeHelper idTypeHelper;
	@Mock
	private AttributesHelper attributeHelper;
	@Mock
	private GroupHelper groupHelper;
	@Mock
	private EntityCredentialsHelper credentialHelper;
	@Mock
	private AuditPublisher audit;
	@InjectMocks
	private IdentityHelper identityHelper;

	@Test
	public void shouldInsertDynamicIdentityWhenAllowSystemIsTrue() throws IllegalIdentityValueException
	{
		IdentityParam entityParam = new IdentityParam("id", "val");
		DynamicIdentityTypeDefinition mock = mock(DynamicIdentityTypeDefinition.class);
		when(idTypesRegistry.getByName("id")).thenReturn(mock);
		when(idTypeHelper.upcastIdentityParam(entityParam, 1)).thenReturn(mock(Identity.class));

		assertThatNoException().isThrownBy(() -> identityHelper.insertIdentity(entityParam, 1, true));
	}

	@Test
	public void shouldNotInsertDynamicIdentityWhenAllowSystemIsFalse()
	{
		IdentityParam entityParam = new IdentityParam("id", "val");
		DynamicIdentityTypeDefinition mockDefinition = mock(DynamicIdentityTypeDefinition.class);
		when(idTypesRegistry.getByName("id")).thenReturn(mockDefinition);
		when(mockDefinition.getId()).thenReturn("id");

		assertThatThrownBy(() -> identityHelper.insertIdentity(entityParam, 1, false))
				.isOfAnyClassIn(IllegalIdentityValueException.class);
	}
}