/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.components;

import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeValueEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ext.StringAttributeHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextOnlyAttributeHandlerTest
{
	@Test
	void shouldAcceptEmptyValueOnOptionalFieldEvenWhenSyntaxRequiresNonEmpty() throws Exception
	{
		// given
		StringAttributeSyntax syntax = new StringAttributeSyntax();
		syntax.setMinLength(1);

		MessageSource msg = Mockito.mock(MessageSource.class);
		Mockito.when(msg.getMessage(Mockito.anyString())).thenReturn("");
		Mockito.when(msg.getMessage(Mockito.anyString(), Mockito.any())).thenReturn("");

		StringAttributeHandler handler = new StringAttributeHandler(msg, syntax);
		AttributeValueEditor editor = handler.getEditorComponent("", "Teamname");

		AttributeEditContext context = AttributeEditContext.builder()
				.withRequired(false)
				.build();

		// when
		editor.getEditor(context);

		// then
		assertThatCode(editor::getCurrentValue).doesNotThrowAnyException();
		assertThat(editor.getCurrentValue()).isEqualTo("");
	}

	@Test
	void shouldRejectEmptyValueOnRequiredFieldWhenSyntaxRequiresNonEmpty() throws Exception
	{
		// given
		StringAttributeSyntax syntax = new StringAttributeSyntax();
		syntax.setMinLength(1);

		MessageSource msg = Mockito.mock(MessageSource.class);
		Mockito.when(msg.getMessage(Mockito.anyString())).thenReturn("");
		Mockito.when(msg.getMessage(Mockito.anyString(), Mockito.any())).thenReturn("");

		StringAttributeHandler handler = new StringAttributeHandler(msg, syntax);
		AttributeValueEditor editor = handler.getEditorComponent("", "Teamname");

		AttributeEditContext context = AttributeEditContext.builder()
				.withRequired(true)
				.build();

		// when
		editor.getEditor(context);

		// then
		assertThatThrownBy(editor::getCurrentValue).isInstanceOf(IllegalAttributeValueException.class);
	}
}
