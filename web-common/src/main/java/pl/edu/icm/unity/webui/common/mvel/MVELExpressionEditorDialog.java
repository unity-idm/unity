/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.mvel;

import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;

class MVELExpressionEditorDialog extends AbstractDialog
{
	public static final int CHEET_SHEET_LINES_COUNT = 4;
	
	private Consumer<String> valueConsumer;
	private TextArea value;
	private Label evalTo;
	private CollapsibleLayout variables;
	private CollapsibleLayout cheatSheet;
	private MVELExpressionContext context;
	private Binder<StringBindingValue> binder;
	
	MVELExpressionEditorDialog(MessageSource msg, MVELExpressionContext context,
			 boolean mandatory, String initValue, Consumer<String> valueConsumer)
	{
		super(msg, msg.getMessage(context.titleKey));
		this.valueConsumer = valueConsumer;
		this.value = new TextArea();
		this.evalTo = new Label();
		this.context = context;
		setSizeMode(SizeMode.MEDIUM);
		init(initValue, mandatory);
		
	
	}
	private void init(String initValue, boolean mandatory)
	{
		evalTo.setCaption(msg.getMessage("MVELExpressionField.evalTo", msg.getMessage(context.evalToKey)));
		evalTo.setCaptionAsHtml(true);
		configureBinding(mandatory);
		value.setValue(initValue);
		value.addStyleName(Styles.fontMonospace.toString());
		value.addStyleName(Styles.textAreaResizable.toString());
		value.addShortcutListener(new ShortcutListener("", ShortcutAction.KeyCode.TAB, null)
		{
			@Override
			public void handleAction(Object sender, Object target)
			{
				insertTab();
			}
		});
	}

	private void insertTab()
	{
		String v = value.getValue() == null ? "" : value.getValue();
		int cursorPos = value.getCursorPosition();
		String st = v.substring(0, cursorPos);
		String fi = v.substring(cursorPos);
		value.setValue(st + "    " + fi);
		value.setCursorPosition(cursorPos + 4);
	}

	void configureBinding(boolean mandatory)
	{
		binder = new Binder<>(StringBindingValue.class);
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

	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSizeFull();
		FormLayout wrapper = new FormLayout(evalTo);
		wrapper.setMargin(false);
		main.addComponent(wrapper);
		main.addComponent(value);
		value.setWidth(100, Unit.PERCENTAGE);

		Link link = new Link();
		link.setCaptionAsHtml(true);
		link.setCaption(msg.getMessage("MVELExpressionField.mvelLinkText"));
		link.setTargetName("_blank");
		link.setResource(new ExternalResource(msg.getMessage("MVELExpressionField.mvelLink")));
		main.addComponent(link);
		
		FormLayoutWithFixedCaptionWidth varsL = FormLayoutWithFixedCaptionWidth.withShortCaptions();
		varsL.setMargin(false);
		varsL.addStyleName("u-mvelFieldEditorLayout");

		context.vars.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach( e ->
		{
			Label var = new Label();
			var.setCaption(e.getKey());
			var.setStyleName(Styles.wordWrap.toString());
			var.setValue(msg.getMessage(e.getValue()));
			varsL.addComponent(var);
		});

		variables = new CollapsibleLayout(msg.getMessage("MVELExpressionField.availableVariables"), varsL);
		variables.expand();
		main.addComponent(variables);

		FormLayoutWithFixedCaptionWidth cheatSheetL =  FormLayoutWithFixedCaptionWidth.withShortCaptions();
		cheatSheetL.setMargin(false);
		cheatSheetL.addStyleName("u-mvelFieldEditorLayout");

		for (int i = 1; i <= CHEET_SHEET_LINES_COUNT; i++)
		{
			Label chLabel = new Label();
			chLabel.setCaption(msg.getMessage("MVELExpressionField.cheetSheet." + i + ".key"));
			chLabel.setValue(msg.getMessage("MVELExpressionField.cheetSheet." + i + ".value"));
			cheatSheetL.addComponent(chLabel);
		}
		cheatSheet = new CollapsibleLayout(msg.getMessage("MVELExpressionField.cheetSheet"), cheatSheetL);
		cheatSheet.collapse();

		main.addComponent(cheatSheet);

		return main;

	}

	@Override
	protected void onConfirm()
	{
		valueConsumer.accept(value.getValue());
		close();
	}
}