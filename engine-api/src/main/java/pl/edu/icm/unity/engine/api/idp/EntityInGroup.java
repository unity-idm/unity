/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * EntityParam with group context information
 * 
 * @author K. Benedyczak
 */
public class EntityInGroup
{
	public final String group;
	public final EntityParam entityParam;
	
	public EntityInGroup(String group, EntityParam entityParam)
	{
		this.group = group;
		this.entityParam = entityParam;
	}
}
