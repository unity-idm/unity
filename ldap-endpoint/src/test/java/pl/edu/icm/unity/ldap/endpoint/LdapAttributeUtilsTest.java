/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 *
 * @author Willem Elbers <willem@clarin.eu>
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapAttributeUtilsTest
{
	@Mock
	public static EntityManagement idmMock;
	private LdapServerProperties ldapServerProperties;
	private LdapServerFacade facade;
	private LdapAttributeUtils utils;

	@Before
	public void initialize() throws Exception
	{
		Properties properties = new Properties();

		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "1." + LdapServerProperties.ATTRIBUTES_MAP_LDAP_AT, "mail");
		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "1." + LdapServerProperties.ATTRIBUTES_MAP_LDAP_OID,
				"0.9.2342.19200300.100.1.3");
		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "1." + LdapServerProperties.ATTRIBUTES_MAP_UNITY_ATRIBUTE, "");
		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "1." + LdapServerProperties.ATTRIBUTES_MAP_UNITY_IDENTITY,
				"email");

		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "2." + LdapServerProperties.ATTRIBUTES_MAP_LDAP_AT,
				"displayName");
		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "2." + LdapServerProperties.ATTRIBUTES_MAP_LDAP_OID,
				"2.16.840.1.113730.3.1.241");
		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "2." + LdapServerProperties.ATTRIBUTES_MAP_UNITY_ATRIBUTE,
				"fullName");
		properties.put(LdapServerProperties.PREFIX + LdapServerProperties.ATTRIBUTES_MAP_PFX
				+ "2." + LdapServerProperties.ATTRIBUTES_MAP_UNITY_IDENTITY, "");

		ldapServerProperties = new LdapServerProperties(properties);
		facade = LdapServerFacadeMockFactory
				.getMockWithServerProperties(ldapServerProperties);
		utils = new LdapAttributeUtils(facade, idmMock, ldapServerProperties);
	}

	/**
	 * Test mapping of unity data to ldap attributes.
	 * 
	 * @throws EngineException
	 * @throws org.apache.directory.api.ldap.model.exception.LdapException
	 */
	@Test
	public void testAddAttribute() throws EngineException, LdapException
	{
		VerifiableEmail vEmail = EmailIdentity.fromIdentityParam(
				new IdentityParam(EmailIdentity.ID, "test@clarin.eu"));
		Identity id = new Identity(EmailIdentity.ID, "test@clarin.eu", 1L,
				vEmail.getComparableValue());

		Entity userEntity = new Entity(Lists.newArrayList(id), new EntityInformation(null),
				null);
		Collection<AttributeExt> unityUserAttrs = new HashSet<>();

		Attribute attr = StringAttribute.of("fullName", "/", "Test user");
		AttributeExt extAttr = new AttributeExt(attr, true);
		unityUserAttrs.add(extAttr);

		// Mock getGroups call
		Map<String, GroupMembership> grps = new HashMap<>();
		grps.put("/clarin", new GroupMembership("/clarin/user", 1L, new Date()));
		when(idmMock.getGroups(new EntityParam(userEntity.getId()))).thenReturn(grps);

		Entry entry = new DefaultEntry(facade.getDs().getSchemaManager());
		utils.addAttribute(SchemaConstants.USER_PASSWORD_AT, userEntity, "test@clarin.eu",
				unityUserAttrs, entry);
		assertThat(entry.getAttributes().size(), is(equalTo(1)));
		Value v = entry.getAttributes().iterator().next().get();
		assertThat(v, is(not(nullValue())));
		assertThat(v.getString(), is(equalTo("not disclosing")));

		entry = new DefaultEntry(facade.getDs().getSchemaManager());
		utils.addAttribute(SchemaConstants.CN_AT, userEntity, "test@clarin.eu",
				unityUserAttrs, entry);
		assertThat(entry.getAttributes().size(), is(equalTo(1)));
		v = entry.getAttributes().iterator().next().get();
		assertThat(v, is(not(nullValue())));
		assertThat(v.getString(), is(equalTo("test@clarin.eu")));

		// Map groups to ldap attribute
		entry = new DefaultEntry(facade.getDs().getSchemaManager());
		utils.addAttribute(LdapAttributeUtils.MEMBER_OF_AT, userEntity, "test@clarin.eu",
				unityUserAttrs, entry);
		assertThat(entry.getAttributes().size(), is(equalTo(1)));
		v = entry.getAttributes().iterator().next().get();
		assertThat(v, is(not(nullValue())));
		assertThat(v.getString(), is(equalTo("cn=user,cn=clarin")));

		// Map identity to ldap attribute
		entry = new DefaultEntry(facade.getDs().getSchemaManager());
		utils.addAttribute(SchemaConstants.MAIL_AT, userEntity, "test@clarin.eu",
				unityUserAttrs, entry);
		assertThat(entry.getAttributes().size(), is(equalTo(1)));
		v = entry.getAttributes().iterator().next().get();
		assertThat(v, is(not(nullValue())));
		assertThat(v.getString(), is(equalTo("test@clarin.eu")));

		// Map unity attribute to ldap attribute
		entry = new DefaultEntry(facade.getDs().getSchemaManager());
		utils.addAttribute(SchemaConstants.DISPLAY_NAME_AT, userEntity, "test@clarin.eu",
				unityUserAttrs, entry);
		assertThat(entry.getAttributes().size(), is(equalTo(1)));
		v = entry.getAttributes().iterator().next().get();
		assertThat(v, is(not(nullValue())));
		assertThat(v.getString(), is(equalTo("Test user")));
	}
}
