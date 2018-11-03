/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.helpers;

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
		if (cancelCaption != null)
			hl.addComponent(createCancelButton(cancelCaption, onCancel));
		hl.addComponent(createConfirmButton(confirmCaption, onConfirm));
		return hl;
	}

	public static Button createConfirmButton(String confirmCaption, Runnable onConfirm)
	{
		Button confirm = new Button(confirmCaption, e -> onConfirm.run());
		confirm.addStyleName("u-dialog-confirm");
		return confirm;
	}

	public static Button createCancelButton(String cancelCaption, Runnable onCancel)
	{
		Button confirm = new Button(cancelCaption, e -> onCancel.run());
		confirm.addStyleName("u-dialog-cancel");
		return confirm;
	}
}
