/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulk;

import java.util.Collections;
import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeExt;

public class EntityGroupAttributes
{
	public final long entityId;
	public final Map<String, AttributeExt> attribtues;
	
	public EntityGroupAttributes(long entityId, Map<String, AttributeExt> attribtues)
	{
		this.entityId = entityId;
		this.attribtues = Collections.unmodifiableMap(attribtues);
	}
}
