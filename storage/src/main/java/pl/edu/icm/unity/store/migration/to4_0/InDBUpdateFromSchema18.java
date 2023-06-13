/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_0;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

@Component
public class InDBUpdateFromSchema18 implements InDBContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema18.class);

	private static final Set<String> unwantedIdentityTypes = Set.of("fidoUserHandle", "persistent", "targetedPersistent", "transient");

	private final RegistrationFormDB formsDB;

	@Autowired
	public InDBUpdateFromSchema18(RegistrationFormDB formsDB)
	{
		this.formsDB = formsDB;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 18;
	}

	@Override
	public void update() throws IOException
	{
		removeFidoIdentityFromForms();
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
}
