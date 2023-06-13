/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.registration.EnquiryForm;

/**
 * Easy access to {@link EnquiryForm} storage.
 * 
 * @author K. Benedyczak
 */
public interface EnquiryFormDB extends NamedCRUDDAOWithTS<EnquiryForm>
{
	void deleteWithoutDependencyChecking(String id);
}
