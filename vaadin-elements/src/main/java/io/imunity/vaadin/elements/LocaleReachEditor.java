/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;

import java.util.Locale;

import static com.wontlost.ckeditor.Constants.*;
import static com.wontlost.ckeditor.Constants.Toolbar.*;

public class LocaleReachEditor extends VerticalLayout implements HasValidation
{
	public final Locale locale;
	public final VaadinCKEditor classicEditor;

	public LocaleReachEditor(Locale locale)
	{
		setSpacing(false);
		setPadding(false);
		getStyle().set("position", "relative");

		this.locale = locale;
		this.classicEditor = new VaadinCKEditorBuilder().with(builder ->
		{
			Config config = new Config();
			config.setEditorToolBar(new Toolbar[] {
					bold, italic, underline, strikethrough, fontBackgroundColor,
					fontFamily, fontSize, fontColor, imageUpload, htmlEmbed,
					code, heading, insertTable, numberedList, bulletedList,
					indent, outdent, mediaEmbed, blockQuote, undo, redo
			});
			config.setUILanguage(locale.getLanguage().equals("en") ? Language.en_gb : Language.valueOf(locale.getLanguage()));
			builder.config = config;
			builder.editorType = EditorType.CLASSIC;
			builder.theme = ThemeType.DARK;
		}).createVaadinCKEditor();

		classicEditor.getStyle().set("margin", "0");
		classicEditor.getStyle().set("padding-top", "0.5em");
		classicEditor.getStyle().set("display", "flex");
		classicEditor.getStyle().set("flex-direction", "column-reverse");
		classicEditor.getStyle().set("width", "103.5%");

		FlagIcon flagIcon = new FlagIcon(locale.getLanguage());
		flagIcon.getStyle().set("position", "absolute");
		flagIcon.getStyle().set("top", "1.2em");
		flagIcon.getStyle().set("right", "-1.5em");
		flagIcon.getStyle().set("z-index", "10");
		add(classicEditor, flagIcon);
	}

	public void setValue(String value) {
		classicEditor.setValue(value);
	}

	public String getValue() {
		return classicEditor.getValue();
	}

	public void setReadOnly(boolean readOnly) {
		classicEditor.setReadOnly(readOnly);
	}

	public Registration addValueChangeListener(HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<CustomField<String>, String>> listener)
	{
		return classicEditor.addValueChangeListener(listener);
	}

	@Override
	public void setErrorMessage(String s)
	{
		classicEditor.setErrorMessage(s);
	}

	@Override
	public String getErrorMessage()
	{
		return classicEditor.getErrorMessage();
	}

	@Override
	public void setInvalid(boolean b)
	{
		classicEditor.setInvalid(b);
		if(b)
			classicEditor.setClassName("invalid");
		else
			classicEditor.removeClassName("invalid");
	}

	@Override
	public boolean isInvalid()
	{
		return classicEditor.isInvalid();
	}
}
