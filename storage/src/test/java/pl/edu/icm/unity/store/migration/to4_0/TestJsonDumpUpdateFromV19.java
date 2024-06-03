/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to4_0;


import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations =
{ "classpath*:META-INF/components.xml" })
public class TestJsonDumpUpdateFromV19
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;

	@Autowired
	protected ImportExport ie;

	@Autowired
	private RegistrationFormDB registrationFormDB;

	@Autowired
	private ObjectStoreDAO genericObjectsDAO;


	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	@Test
	public void testImportFrom4_0_0()
	{
		tx.runInTransaction(() ->
		{
			try
			{
				ie.load(new BufferedInputStream(
						new FileInputStream("src/test/resources/updateData/from4.0.x/" + "testbed-from4.0.0.json")));
			} catch (Exception e)
			{
				fail("Import failed " + e);
			}
			checkFidoIdentityHasBeenRemoved();
			checkHomeUIConfigurationMigration();
			checkOAuthIconsMigration();
		});
	}
	
	private void checkFidoIdentityHasBeenRemoved()
	{
		registrationFormDB.getAll().stream()
				.flatMap(form -> form.getIdentityParams().stream())
				.filter(identity -> identity.getIdentityType().equals("fidoUserHandle"))
				.findAny().ifPresent(identity -> fail("fidoUserHandle identity should be removed"));
	}

	private void checkHomeUIConfigurationMigration()
	{
		List<GenericObjectBean> endpoints = genericObjectsDAO.getObjectsOfType(EndpointHandler.ENDPOINT_OBJECT_TYPE);
		for(GenericObjectBean endpoint : endpoints)
		{
			ObjectNode node = JsonUtil.parse(endpoint.getContents());
			if (node.get("typeId").textValue().equals("UserHomeUI"))
			{
				String conf = node.get("configuration").get("configuration").textValue();
				assertThat(conf).contains("unity.userhome.disabledComponents.10=trustedDevices") ;
				assertThat(conf).doesNotContain("unity.userhome.disabledComponents.3=userInfo") ;
				assertThat(conf).doesNotContain("unity.userhome.disabledComponents.4=identitiesManagement") ;
			}
		}

	}
	
	private void checkOAuthIconsMigration()
	{
		List<GenericObjectBean> authenticators = genericObjectsDAO
				.getObjectsOfType(AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE);
		for (GenericObjectBean authenticator : authenticators)
		{
			ObjectNode node = JsonUtil.parse(authenticator.getContents());
			if (node.get("verificationMethod")
					.textValue()
					.equals("oauth2")
					&& node.get("name")
							.asText()
							.equals("oauth"))
			{
				String conf = node.get("configuration")
						.textValue();
				assertThat(conf)
						.contains("unity.oauth2.client.providers.local.iconUrl=assets/img/other/logo-square.png");
			}
		}

	}

}
