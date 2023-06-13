/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.endpoint.Endpoint;

/**
 * Easy access to {@link Endpoint} storage.
 * 
 * @author K. Benedyczak
 */
public interface EndpointDB extends NamedCRUDDAOWithTS<Endpoint>
{
}
