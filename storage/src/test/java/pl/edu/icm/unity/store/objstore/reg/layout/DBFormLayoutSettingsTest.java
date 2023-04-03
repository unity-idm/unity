/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBFormLayoutSettingsTest extends DBTypeTestBase<DBFormLayoutSettings>
{

	@Override
	protected String getJson()
	{
		return "{\"compactInputs\":true,\"showCancel\":true,\"columnWidth\":10.0,\"columnWidthUnit\":\"EM\","
				+ "\"logoURL\":\"logo\"}\n";
	}

	@Override
	protected DBFormLayoutSettings getObject()
	{
		return DBFormLayoutSettings.builder()
				.withCompactInputs(true)
				.withShowCancel(true)
				.withColumnWidth(10)
				.withColumnWidthUnit("EM")
				.withLogoURL("logo")
				.build();
	}

}
