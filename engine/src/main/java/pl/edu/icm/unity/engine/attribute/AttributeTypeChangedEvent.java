/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.event.Event;

/**
 * Triggered when AttributeType is modified - hold both instances before modification and after.
 *
 * @author R. Ledzinski
 */
public class AttributeTypeChangedEvent implements Event
{
	public final AttributeType oldAT;
	public final AttributeType newAT;

	public AttributeTypeChangedEvent(AttributeType oldAT, AttributeType newAT) {
		this.oldAT = oldAT;
		this.newAT = newAT;
	}
}
