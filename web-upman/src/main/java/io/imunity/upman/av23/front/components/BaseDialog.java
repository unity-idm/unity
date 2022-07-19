/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;

public class BaseDialog extends Dialog
{
	public BaseDialog(String headerTxt, String cancelTxt, HtmlContainer dialogHolder)
	{
		setHeaderTitle(headerTxt);
		setResizable(true);
		Button cancelButton = new Button(cancelTxt, e -> close());
		getFooter().add(cancelButton);
		dialogHolder.add(this);
	}
}
