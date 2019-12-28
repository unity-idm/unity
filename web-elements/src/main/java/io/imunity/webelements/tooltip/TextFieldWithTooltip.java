/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.webelements.tooltip;

import com.vaadin.ui.TextField;

public class TextFieldWithTooltip extends CustomFieldWithTooltip<String, TextField>
{
	public TextFieldWithTooltip(String caption, String tooltipInfo)
	{
		super(TextField::new, caption, tooltipInfo);
	}
}
