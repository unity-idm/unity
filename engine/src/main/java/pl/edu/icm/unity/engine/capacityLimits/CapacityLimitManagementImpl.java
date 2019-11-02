/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimits;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CapacityLimitManagement;
import pl.edu.icm.unity.store.api.generic.CapacityLimitDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimitName;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class CapacityLimitManagementImpl implements CapacityLimitManagement
{
	private CapacityLimitDB limitDB;

	@Autowired
	public CapacityLimitManagementImpl(CapacityLimitDB limitDB)
	{
		this.limitDB = limitDB;
	}
	
	@Transactional
	@Override
	public void setLimit(CapacityLimit limit)
	{
		try
		{
			limitDB.get(limit.getName());
			limitDB.update(limit);
		} catch (IllegalArgumentException e)
		{
			limitDB.create(limit);
		}
	}
	
	@Transactional
	@Override
	public CapacityLimit getLimit(CapacityLimitName name)
	{
		return limitDB.get(name.toString());
	}
	
	@Transactional
	@Override
	public Set<CapacityLimit> getAllLimits()
	{
		return limitDB.getAll().stream().collect(Collectors.toSet());
	}
}
