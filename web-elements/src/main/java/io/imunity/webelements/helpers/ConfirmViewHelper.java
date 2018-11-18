/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.helpers;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;

/**
 * Helper for creating confirm view.
 * @author P.Piernik
 *
 */
public class ConfirmViewHelper
{
	public static HorizontalLayout getConfirmButtonsBar(String confirmCaption,
			String cancelCaption, Runnable onConfirm, Runnable onCancel)
	{

		HorizontalLayout hl = new HorizontalLayout();
		hl.setMargin(new MarginInfo(true, false));
		hl.addComponent(createConfirmButton(confirmCaption, onConfirm));
		if (cancelCaption != null)
			hl.addComponent(createCancelButton(cancelCaption, onCancel));
		return hl;
	}

	public static Button createConfirmButton(String confirmCaption, Runnable onConfirm)
	{
		Button confirm = new Button(confirmCaption, e -> onConfirm.run());
		confirm.addStyleName("u-view-save");
		return confirm;
	}

	public static Button createCancelButton(String cancelCaption, Runnable onCancel)
	{
		Button confirm = new Button(cancelCaption, e -> onCancel.run());
		confirm.addStyleName("u-view-close");
		return confirm;
	}
}
