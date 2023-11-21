/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.policy_documents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;

class PolicyUpdateConfirmationDialog extends Dialog
{
	PolicyUpdateConfirmationDialog(String header, String txt, String saveTxt, Runnable saveRunnable, String saveSilently,
								   Runnable saveSilentlyRunnable, String cancelTxt)
	{
		setWidth("40em");
		getHeader().add(new Span(header));
		add(new Span(txt));
		Button saveButton = new Button(saveTxt);
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		saveButton.addClickListener(e ->
		{
			saveRunnable.run();
			close();
		});
		Button cancelButton = new Button(cancelTxt);
		cancelButton.addClickListener(e -> close());
		Button saveSilentlyButton = new Button(saveSilently);
		saveSilentlyButton.addClickListener(e ->
		{
			saveSilentlyRunnable.run();
			close();
		});
		getFooter().add(cancelButton, saveSilentlyButton, saveButton);
	}
}
