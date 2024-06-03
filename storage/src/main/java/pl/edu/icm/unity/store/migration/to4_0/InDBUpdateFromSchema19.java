/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_0;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InDBUpdateFromSchema19 implements InDBContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema19.class);

	private static final Set<String> unwantedIdentityTypes = Set.of("fidoUserHandle", "persistent", "targetedPersistent", "transient");

	private final RegistrationFormDB formsDB;
	private final ObjectStoreDAO genericObjectsDAO;


	@Autowired
	public InDBUpdateFromSchema19(RegistrationFormDB formsDB, ObjectStoreDAO genericObjectsDAO)
	{
		this.formsDB = formsDB;
		this.genericObjectsDAO = genericObjectsDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 19;
	}

	@Override
	public void update() throws IOException
	{
		removeFidoIdentityFromForms();
		migrateHomeUiDisabledComponentsConfiguration();
		updateAuthenticatorIcons();
	}

	public void migrateHomeUiDisabledComponentsConfiguration()
	{
		List<GenericObjectBean> endpoints = genericObjectsDAO.getObjectsOfType(EndpointHandler.ENDPOINT_OBJECT_TYPE);
		for(GenericObjectBean endpoint : endpoints)
		{
			ObjectNode node = JsonUtil.parse(endpoint.getContents());
			Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration(node);
			configuration.ifPresent(conf ->
			{
				((ObjectNode) node.get("configuration")).set("configuration", conf);
				endpoint.setContents(JsonUtil.serialize2Bytes(node));
				genericObjectsDAO.updateByKey(endpoint.getId(), endpoint);
			});
		}
	}

	void removeFidoIdentityFromForms()
	{
		List<RegistrationForm> all = formsDB.getAll();
		for (RegistrationForm registrationForm : all)
		{
			List<IdentityRegistrationParam> toRemove = registrationForm.getIdentityParams().stream()
					.filter(param -> unwantedIdentityTypes.contains(param.getIdentityType()))
					.toList();
			if (registrationForm.getIdentityParams().removeAll(toRemove))
			{
				formsDB.update(registrationForm);
				log.info("Those identity params {} has been removed from registration form {}",
						toRemove.stream().map(IdentityRegistrationParam::getIdentityType).collect(Collectors.toList()),
						registrationForm.getName()
				);
			}
		}
	}
	
	void updateAuthenticatorIcons()
	{
		List<GenericObjectBean> genereicObjects = genericObjectsDAO
				.getObjectsOfType(AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE);
		for (GenericObjectBean genericObject : genereicObjects)
		{
			ObjectNode objContent = JsonUtil.parse(genericObject.getContents());
			UpdateHelperTo4_0.updateOAuthAuthenticatorIcons(objContent)
					.ifPresent(o ->
					{
						genericObject.setContents(JsonUtil.serialize2Bytes(o));
						genericObjectsDAO.updateByKey(genericObject.getId(), genericObject);
					});

		}
	}
}
