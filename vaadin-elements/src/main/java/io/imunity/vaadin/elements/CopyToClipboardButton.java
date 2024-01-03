/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Function;

import static io.imunity.vaadin.elements.CssClassNames.POINTER;

public class CopyToClipboardButton extends Div
{
	private static final NotificationPresenter notificationPresenter = new NotificationPresenter();

	public CopyToClipboardButton(Function<String, String> msg, TextField field)
	{
		UI.getCurrent().getPage().addJavaScript("../unitygw/copytoclipboard.js");
		Icon icon = VaadinIcon.COPY.create();
		icon.setTooltipText(msg.apply("CopyToClipboardButton.copyToClipboard"));
		icon.addClickListener(
				e -> UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", field.getValue())
		);
		icon.addClickListener(
				e -> notificationPresenter.showSuccess(msg.apply("CopyToClipboardButton.successCopiedToClipboard"))
		);
		icon.addClassName(POINTER.name());
		add(icon);
	}
}
