/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.components;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import io.imunity.vaadin.elements.FlagIcon;
import org.vaadin.tinymce.TinyMce;

import java.util.Locale;

@JsModule("./tinymce_addon/tinymce/tinymce-loader.js")
public class LocalizedRichTextEditor extends VerticalLayout
{
	public final Locale locale;
	public final TinyMce tinyMce;

	public LocalizedRichTextEditor(Locale locale)
	{
		setSpacing(false);
		setPadding(false);
		addClassName("u-rich-editor");

		this.locale = locale;
		this.tinyMce = new TinyMce();
		tinyMce.configure("branding", false)
				.configure("skin", false)
				.configure("content_css", false)
				.configure("menubar", "insert format")
				.configure("plugins", "link", "image", "lists", "advlist", "table")
				.configure("toolbar",
						"undo redo | bold italic underline strikethrough superscript subscript | forecolor backcolor | bullist numlist | link image | table",
						"blocks fontfamily fontsize | align outdent indent | removeformat")
				.configure("block_formats",
						"Paragraph=p; Plain text=div; Heading 1=h1; Heading 2=h2; Heading 3=h3; Heading 4=h4; Heading 5=h5; Heading 6=h6; Preformatted=pre;");
		tinyMce.addClassName("u-rich-editor-tinymc");

		FlagIcon flagIcon = new FlagIcon(locale.getLanguage());
		flagIcon.addClassName("u-rich-editor-flag-icon");
		add(tinyMce, flagIcon);
	}

	public void setValue(String value)
	{
		tinyMce.setValue(value);
	}

	public String getValue()
	{
		return tinyMce.getValue();
	}

	public void setReadOnly(boolean readOnly)
	{
		tinyMce.setReadOnly(readOnly);
	}

	public Registration addValueChangeListener(
			HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<TinyMce, String>> listener)
	{
		return tinyMce.addValueChangeListener(listener);
	}
}
