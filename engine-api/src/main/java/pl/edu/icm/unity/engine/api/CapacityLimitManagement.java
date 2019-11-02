/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api;

import java.util.Set;

import pl.edu.icm.unity.types.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimitName;

/**
 * 
 * @author P.Piernik
 *
 */
public interface CapacityLimitManagement
{
	void setLimit(CapacityLimit limit);
	
	CapacityLimit getLimit(CapacityLimitName name);
	
	Set<CapacityLimit> getAllLimits();
}
