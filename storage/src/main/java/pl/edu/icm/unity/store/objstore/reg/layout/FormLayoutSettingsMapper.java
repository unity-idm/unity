/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

public class FormLayoutSettingsMapper
{

	public static DBFormLayoutSettings map(FormLayoutSettings formLayoutSettings)
	{
		return DBFormLayoutSettings.builder()
				.withCompactInputs(formLayoutSettings.isCompactInputs())
				.withColumnWidth(formLayoutSettings.getColumnWidth())
				.withColumnWidthUnit(formLayoutSettings.getColumnWidthUnit())
				.withShowCancel(formLayoutSettings.isShowCancel())
				.withLogoURL(formLayoutSettings.getLogoURL())
				.build();

	}

	public static FormLayoutSettings map(DBFormLayoutSettings restFormLayoutSettings)
	{
		return new FormLayoutSettings(restFormLayoutSettings.compactInputs, restFormLayoutSettings.columnWidth,
				restFormLayoutSettings.columnWidthUnit, restFormLayoutSettings.showCancel,
				restFormLayoutSettings.logoURL);
	}

}
