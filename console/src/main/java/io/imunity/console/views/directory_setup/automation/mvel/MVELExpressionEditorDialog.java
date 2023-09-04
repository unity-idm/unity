/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.automation.mvel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.InputLabel;
import io.imunity.vaadin.elements.StringBindingValue;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

import java.util.function.Consumer;

class MVELExpressionEditorDialog extends Dialog
{
	private static final int CHEET_SHEET_LINES_COUNT = 4;
	private static final String MONOSPACE_FONT = "Consolas";
	private final MessageSource msg;
	private final Consumer<String> valueConsumer;
	private final TextArea value;
	private final MVELExpressionContext context;
	private InputLabel inputLabel;

	MVELExpressionEditorDialog(MessageSource msg, MVELExpressionContext context,
			 boolean mandatory, String initValue, Consumer<String> valueConsumer)
	{
		this.msg = msg;
		setHeaderTitle(msg.getMessage(context.titleKey));
		this.valueConsumer = valueConsumer;
		this.value = new TextArea();
		this.context = context;
		init(initValue, mandatory);
		Button closeButton = new Button(msg.getMessage("close"), e -> close());
		Button okButton = new Button(msg.getMessage("ok"), e -> onConfirm());
		okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		getFooter().add(closeButton, okButton);
		add(getContents());
		setWidth("50em");
		setHeight("30em");
	}
	private void init(String initValue, boolean mandatory)
	{
		configureBinding(mandatory);
		inputLabel = new InputLabel("");
		inputLabel.getElement().setProperty("innerHTML", msg.getMessage("MVELExpressionField.evalTo", msg.getMessage(context.evalToKey)));
		value.setValue(initValue);
		value.getStyle().set("font-family", MONOSPACE_FONT);
		value.getStyle().set("resize", "vertical");
		value.getStyle().set("overflow", "auto");
		value.setWidth("30em");
		value.setMinHeight("5em");
		value.getElement().executeJs(
			"""
				document.querySelector("textarea").addEventListener('keydown', function(e) {
					if (e.key == 'Tab') {
						e.preventDefault();
						var start = this.selectionStart;
						var end = this.selectionEnd;

						this.value = this.value.substring(0, start) + "    " + this.value.substring(end);
						this.selectionStart = this.selectionEnd = start + 4;
					}
				});
			"""
		);
		Shortcuts.addShortcutListener(value, () -> {}, Key.TAB).listenOn(value);
	}

	void configureBinding(boolean mandatory)
	{
		Binder<StringBindingValue> binder = new Binder<>(StringBindingValue.class);
		if (mandatory)
		{
			binder.forField(value).withValidator(MVELExpressionEditor.getValidator(msg, mandatory))
					.asRequired(msg.getMessage("fieldRequired")).bind("value");
					
		} else
		{
			binder.forField(value).withValidator(MVELExpressionEditor.getValidator(msg, mandatory)).bind("value");
		}
		binder.setBean(new StringBindingValue(""));
	}

	protected Component getContents()
	{
		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.setSpacing(false);

		Anchor link = new Anchor(msg.getMessage("MVELExpressionField.mvelLink"), msg.getMessage("MVELExpressionField.mvelLinkText"));
		link.setTarget("_blank");
		
		FormLayout varsL = new FormLayout();
		varsL.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		context.vars.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach( e ->
			varsL.addFormItem(new Label(msg.getMessage(e.getValue())), new Label(e.getKey()))
		);

		Details variables = new Details(msg.getMessage("MVELExpressionField.availableVariables"), varsL);
		variables.setOpened(true);

		FormLayout cheatSheetL = new FormLayout();
		cheatSheetL.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		for (int i = 1; i <= CHEET_SHEET_LINES_COUNT; i++)
		{
			cheatSheetL.addFormItem(
					new Label(msg.getMessage("MVELExpressionField.cheetSheet." + i + ".value")),
					new Label(msg.getMessage("MVELExpressionField.cheetSheet." + i + ".key"))
			);
		}
		Details cheatSheet = new Details(msg.getMessage("MVELExpressionField.cheetSheet"), cheatSheetL);
		cheatSheet.setOpened(true);

		main.add(inputLabel, value, link, variables, cheatSheet);

		return main;
	}

	protected void onConfirm()
	{
		valueConsumer.accept(value.getValue());
		close();
	}
}