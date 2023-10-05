/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;

class PreviewDialog extends Dialog
{
	PreviewDialog(String html, boolean displayAsHTML)
	{
		Button closeButton = new Button(new Icon("lumo", "cross"),
				(e) -> close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		getHeader().add(closeButton);
		if(displayAsHTML)
			add(new Html("<div>" + html + "</div>"));
		else
			add(new Span(html));
		setResizable(true);
	}
}
