/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Easy access to {@link RegistrationForm} storage.
 * 
 * @author K. Benedyczak
 */
public interface RegistrationFormDB extends NamedCRUDDAOWithTS<RegistrationForm>
{
	void deleteWithoutDependencyChecking(String id);
}
