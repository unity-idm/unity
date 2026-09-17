/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.TextFieldWithVerifyButton;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ConfirmationInfoFormatter;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorContext;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;

class EmailIdentityEditorTest
{
	@Test
	void shouldCreateEditorWithEmptyInitialValue()
	{
		// given
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(anyString()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		EmailIdentityEditor editor = new EmailIdentityEditor(
				messageSource,
				mock(EmailConfirmationManager.class),
				mock(EntityResolver.class),
				mock(ConfirmationInfoFormatter.class),
				mock(NotificationPresenter.class));
		IdentityEditorContext context = IdentityEditorContext.builder().build();

		// when
		TextFieldWithVerifyButton field = (TextFieldWithVerifyButton) editor.getEditor(context).getComponents()[0];

		// then
		assertThat(field.getValue()).isEmpty();
	}
}
