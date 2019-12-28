/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.webelements.tooltip;

import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.widgets.DescriptionTextField;

public class DescriptionTextFieldWithTooltip extends CustomFieldWithTooltip<String, DescriptionTextField>
{
	public DescriptionTextFieldWithTooltip(String caption, String tooltipInfo)
	{
		super(DescriptionTextField::new, caption, tooltipInfo);
		setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
	}
}
