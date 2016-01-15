/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.test;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;

public class I18nJsonTest
{

	@Test
	public void serializationIsReversible()
	{
		I18nString tested = new I18nString("defVal");
		tested.addValue("pl", "val1");
		tested.addValue("en", "val2");
		ObjectNode json = I18nStringJsonUtil.toJson(tested);
		I18nString ret = I18nStringJsonUtil.fromJson(json);
		Assert.assertEquals(tested, ret);
	}
}
