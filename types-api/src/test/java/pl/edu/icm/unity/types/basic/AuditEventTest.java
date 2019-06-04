package pl.edu.icm.unity.types.basic;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;
import pl.edu.icm.unity.types.basic.audit.AuditEvent.AuditEventBuilder;
import pl.edu.icm.unity.types.basic.audit.EventAction;
import pl.edu.icm.unity.types.basic.audit.EventType;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class AuditEventTest
{
	@Test
	public void shouldInitializeTags()
	{
		// given
		AuditEvent event = getEvent();

		// than
		assertThat(event.getTags().size(), is(2));
		assertThat(event.getTags(), hasItem("TAG1"));
		assertThat(event.getTags(), hasItem("TAG2"));
	}

	@Test
	public void shouldBeValidWithAllFields()
	{
		AuditEvent event = getEvent();
	}

	@Test
	public void shouldBeValidWithNullableFields()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.subject(null);
		eventBuilder.details(null);

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		Assertions.assertThat(ex).isNull();
	}

	@Test
	public void shouldBeValidWithNoTags()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.tags(Collections.emptySet());

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		Assertions.assertThat(ex).isNull();
	}

	@Test
	public void shouldRejectNullName()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.name(null);

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		assertThat(ex, instanceOf(NullPointerException.class));
		assertThat(ex.getMessage(), is("AuditEvent.name field is required!"));
	}

	@Test
	public void shouldRejectNullType()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.type(null);

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		assertThat(ex, instanceOf(NullPointerException.class));
		assertThat(ex.getMessage(), is("AuditEvent.type field is required!"));
	}

	@Test
	public void shouldRejectNullTimestamp()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.timestamp(null);

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		assertThat(ex, instanceOf(NullPointerException.class));
		assertThat(ex.getMessage(), is("AuditEvent.timestamp field is required!"));

	}

	@Test
	public void shouldRejectNullInitiator()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.initiator(null);

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		assertThat(ex, instanceOf(NullPointerException.class));
		assertThat(ex.getMessage(), is("AuditEvent.initiator field is required!"));
	}

	@Test
	public void shouldRejectNullAction()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.action(null);

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		assertThat(ex, instanceOf(NullPointerException.class));
		assertThat(ex.getMessage(), is("AuditEvent.action field is required!"));
	}

	@Test
	public void shouldRejectNullTags()
	{
		// given
		AuditEventBuilder eventBuilder = getEventBuilder();
		eventBuilder.tags((Set<String>) null);

		// when
		Throwable ex = Assertions.catchThrowable(() -> eventBuilder.build());

		// than
		assertThat(ex, instanceOf(NullPointerException.class));
		assertThat(ex.getMessage(), is("AuditEvent.tags field is required!"));
	}

	private AuditEvent getEvent()
	{
		return getEventBuilder().build();
	}

	private AuditEventBuilder getEventBuilder()
	{
		return AuditEvent.builder()
				.name("Name")
				.type(EventType.IDENTITY)
				.timestamp(new Date())
				.action(EventAction.ADD)
				.details(JsonUtil.parse("{}"))
				.initiator(new AuditEntity(0l, "Initiator name", "initiator@example.com"))
				.subject(new AuditEntity(1l, "Subject name", "subject@example.com"))
				.tags("TAG1", "TAG2");
	}
}
