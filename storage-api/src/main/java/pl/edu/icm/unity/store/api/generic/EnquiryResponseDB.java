/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.registration.EnquiryResponseState;

/**
 * Easy access to {@link EnquiryResponseState} storage.
 * 
 * @author K. Benedyczak
 */
public interface EnquiryResponseDB extends NamedCRUDDAOWithTS<EnquiryResponseState>
{
}
