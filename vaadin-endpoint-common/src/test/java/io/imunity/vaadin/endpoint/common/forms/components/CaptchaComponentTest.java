package io.imunity.vaadin.endpoint.common.forms.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextField;

import nl.captcha.Captcha;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;

class CaptchaComponentTest
{
	private MessageSource messages;
	private UI ui;

	@BeforeEach
	void configureMessages()
	{
		ui = new UI();
		UI.setCurrent(ui);
		messages = mock(MessageSource.class);
		when(messages.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@AfterEach
	void clearUi()
	{
		UI.setCurrent(null);
	}

	@Test
	void shouldAcceptGeneratedAnswerIgnoringCase()
	{
		// given
		CapturingFactory images = new CapturingFactory();
		CaptchaComponent component = new CaptchaComponent(messages, 6, false, images);
		TextField answer = (TextField) component.getFocusTarget();
		answer.setValue(images.latest.getAnswer().toUpperCase(Locale.ROOT));

		// when / then
		assertThatCode(component::verify).doesNotThrowAnyException();
		assertThat(answer.isInvalid()).isFalse();
		assertThat(images.generated).isEqualTo(1);
	}

	@Test
	void shouldRegenerateAfterIncorrectAnswerAndAcceptOnlyTheCurrentChallenge()
	{
		// given
		CapturingFactory images = new CapturingFactory();
		CaptchaComponent component = new CaptchaComponent(messages, 6, true, images);
		TextField answer = (TextField) component.getFocusTarget();
		Captcha previous = images.latest;
		answer.setValue("incorrect");

		// when
		assertThatThrownBy(component::verify).isInstanceOf(WrongArgumentException.class);

		// then
		assertThat(images.latest).isNotSameAs(previous);
		assertThat(images.generated).isEqualTo(2);
		assertThat(answer.isInvalid()).isTrue();
		assertThat(answer.getValue()).isEmpty();
		answer.setValue(images.latest.getAnswer());
		assertThatCode(component::verify).doesNotThrowAnyException();
		assertThat(answer.isInvalid()).isFalse();
	}

	@Test
	void shouldClearInvalidStateAndGenerateAnotherChallengeOnReset()
	{
		// given
		CapturingFactory images = new CapturingFactory();
		CaptchaComponent component = new CaptchaComponent(messages, 4, false, images);
		TextField answer = (TextField) component.getFocusTarget();
		answer.setValue("partial");
		component.setInvalid();
		Captcha previous = images.latest;

		// when
		component.reset();

		// then
		assertThat(images.latest).isNotSameAs(previous);
		assertThat(images.generated).isEqualTo(2);
		assertThat(answer.getValue()).isEmpty();
		assertThat(answer.isInvalid()).isFalse();
		answer.setValue(images.latest.getAnswer());
		assertThatCode(component::verify).doesNotThrowAnyException();
	}

	private static class CapturingFactory extends CaptchaImageFactory
	{
		private Captcha latest;
		private int generated;

		@Override
		public Captcha create(int length)
		{
			latest = super.create(length);
			generated++;
			return latest;
		}
	}
}
