/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration.layout;

import io.imunity.rest.api.types.registration.layout.RestFormLayoutSettings;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

public class FormLayoutSettingsMapper
{

	public static RestFormLayoutSettings map(FormLayoutSettings formLayoutSettings)
	{
		return RestFormLayoutSettings.builder()
				.withCompactInputs(formLayoutSettings.isCompactInputs())
				.withColumnWidth(formLayoutSettings.getColumnWidth())
				.withColumnWidthUnit(formLayoutSettings.getColumnWidthUnit())
				.withShowCancel(formLayoutSettings.isShowCancel())
				.withLogoURL(formLayoutSettings.getLogoURL())
				.build();

	}

	public static FormLayoutSettings map(RestFormLayoutSettings restFormLayoutSettings)
	{
		return new FormLayoutSettings(restFormLayoutSettings.compactInputs, restFormLayoutSettings.columnWidth,
				restFormLayoutSettings.columnWidthUnit, restFormLayoutSettings.showCancel,
				restFormLayoutSettings.logoURL);
	}

}
