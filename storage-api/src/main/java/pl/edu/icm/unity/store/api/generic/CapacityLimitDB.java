/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.capacity_limit.CapacityLimit;

/**
 * Easy access to {@link CapacityLimit} storage.
 * 
 * @author P.Piernik
 *
 */
public interface CapacityLimitDB extends NamedCRUDDAOWithTS<CapacityLimit>
{
}
