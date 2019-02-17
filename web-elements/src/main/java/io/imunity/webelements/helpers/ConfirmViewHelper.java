/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.helpers;

import com.vaadin.ui.Alignment;
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

		HorizontalLayout main = new HorizontalLayout();
		main.setMargin(false);
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponent(createConfirmButton(confirmCaption, onConfirm));
		if (cancelCaption != null)
			hl.addComponent(createCancelButton(cancelCaption, onCancel));
		main.addComponent(hl);
		main.setComponentAlignment(hl, Alignment.MIDDLE_LEFT);
		return main;
	}

	public static Button createConfirmButton(String confirmCaption, Runnable onConfirm)
	{
		Button confirm = new Button(confirmCaption, e -> onConfirm.run());
		confirm.addStyleName("u-button-form");
		confirm.addStyleName("u-button-action");
		return confirm;
	}

	public static Button createCancelButton(String cancelCaption, Runnable onCancel)
	{
		Button confirm = new Button(cancelCaption, e -> onCancel.run());
		confirm.addStyleName("u-button-form");
		return confirm;
	}
}
