/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.msg;

import pl.edu.icm.unity.MessageArea;

public interface MessageAreaProvider
{
	String getName();
	MessageArea getMessageArea();
}
