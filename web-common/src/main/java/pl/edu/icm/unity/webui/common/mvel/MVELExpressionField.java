/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.mvel;

import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.data.ValueProvider;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Setter;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;

/**
 * Plain text field allowing for editing an MVEL expression
 * 
 * @author K. Benedyczak
 */
public class MVELExpressionField extends CustomField<String>
{
	private MVELExpressionEditor editor;
	private TextField field;
	private Button editorButton;
	private HorizontalLayout layout;
	private String value;
	private boolean mandatory;

	public MVELExpressionField(MessageSource msg, String caption, String description, MVELExpressionContext context)
	{
		this.field = new TextField();
		this.editorButton = new Button(Images.cogs.getResource());
		editorButton.addStyleName(Styles.toolbarButton.toString());
		editorButton.addStyleName(Styles.vButtonLink.toString());
		editorButton.addClickListener(
				e -> new MVELExpressionEditorDialog(msg, context, mandatory, field.getValue(), v -> field.setValue(v)).show());
		this.editor = new MVELExpressionEditor(this, msg);

		layout = new HorizontalLayout();
		layout.setSpacing(false);
		layout.addComponent(field);
		layout.addComponent(editorButton);
		layout.setExpandRatio(field, 1);
		layout.setExpandRatio(editorButton, 0);
		layout.setComponentAlignment(editorButton, Alignment.MIDDLE_RIGHT);
		setCaption(caption);
		setDescription(description, ContentMode.HTML);
		field.addValueChangeListener(e ->
		{
			value = e.getValue();
			fireEvent(new ValueChangeEvent<>(this, getValue(), e.isUserOriginated()));
		});

	}

	public void configureBinding(Binder<?> binder, String fieldName, boolean mandatory)
	{
		this.mandatory = mandatory;
		editor.configureBinding(binder, fieldName, mandatory);
	}

	public <T> void configureBinding(Binder<String> binder, ValueProvider<String, String> getter,
			Setter<String, String> setter, boolean mandatory)
	{
		this.mandatory = mandatory;
		editor.configureBinding(binder, getter, setter, mandatory);
	}

	@Override
	public String getValue()
	{
		return value;
	}
	
	@Override
	public String getEmptyValue()
	{
		return "";
	}

	@Override
	protected Component initContent()
	{
		return layout;
	}

	@Override
	protected void doSetValue(String value)
	{
		if (value != null )
			field.setValue(value);	
		this.value = value;
	}

	protected void addBlurListener(BlurListener listener)
	{
		field.addBlurListener(listener);
	}
	
	@Override
	public void focus()
	{
		field.focus();
	}
	
	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		super.setComponentError(componentError);
		if (componentError != null)
			field.addStyleName("error");
		else
			field.removeStyleName("error");
	}
	
	@Override
	public void setStyleName(String style)
	{
		field.addStyleName(style);
		editorButton.addStyleName(style);
		layout.addStyleName(style);
	}
	
	public static class MVELExpressionEditorDialog extends AbstractDialog
	{
		public static final int CHEET_SHEET_LINES_COUNT = 4;
		
		private Consumer<String> valueConsumer;
		private TextArea value;
		private Label evalTo;
		private CollapsibleLayout variables;
		private CollapsibleLayout cheatSheet;
		private MVELExpressionContext context;
		private Binder<StringBindingValue> binder;
		
		public MVELExpressionEditorDialog(MessageSource msg, MVELExpressionContext context,
				 boolean mandatory, String initValue, Consumer<String> valueConsumer)
		{
			super(msg, msg.getMessage(context.titleKey));
			this.valueConsumer = valueConsumer;
			this.value = new TextArea();
			this.evalTo = new Label();
			this.context = context;
			
			evalTo.setValue(msg.getMessage("MVELExpressionField.evalTo") + " " + msg.getMessage(context.evalToKey));
			configureBinding(mandatory);
			value.setValue(initValue);
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
			main.setSizeFull();
			main.addComponent(evalTo);
			main.addComponent(value);
			value.setWidth(100, Unit.PERCENTAGE);

			FormLayoutWithFixedCaptionWidth varsL = new FormLayoutWithFixedCaptionWidth();
			varsL.setMargin(false);

			context.vars.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach( e ->
			{
				Label var = new Label();
				var.setCaption(e.getKey());
				var.setValue(msg.getMessage(e.getValue()));
				varsL.addComponent(var);
			});

			variables = new CollapsibleLayout(msg.getMessage("MVELExpressionField.availableVariables"), varsL);
			variables.expand();
			main.addComponent(variables);

			FormLayoutWithFixedCaptionWidth cheatSheetL = new FormLayoutWithFixedCaptionWidth();
			cheatSheetL.setMargin(false);
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
}
