/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.webelements.tooltip;

import com.vaadin.ui.ComboBox;

public class ComboBoxWithTooltip<T> extends CustomFieldWithTooltip<T, ComboBox<T>>
{
	public ComboBoxWithTooltip(String caption, String tooltipInfo)
	{
		super(ComboBox::new, caption, tooltipInfo);
	}

	public void setItems(T[] values)
	{
		getField().setItems(values);
	}

	public void setEmptySelectionAllowed(boolean emptySelectionAllowed)
	{
		getField().setEmptySelectionAllowed(emptySelectionAllowed);
	}
}
