package pl.edu.icm.unity.types.basic;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;
import pl.edu.icm.unity.types.basic.audit.AuditEvent.AuditEventBuilder;
import pl.edu.icm.unity.types.basic.audit.EventAction;
import pl.edu.icm.unity.types.basic.audit.EventType;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class AuditEventTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldInitializeTags()
    {
        AuditEvent event = getEvent();
        assertEquals(2, event.getTags().size());
        assertTrue(event.getTags().contains("TAG1"));
        assertTrue(event.getTags().contains("TAG2"));
    }

    @Test
    public void shouldBeValidWithAllFields()
    {
        AuditEvent event = getEvent();
    }

    @Test
    public void shouldBeValidWithNullableFields()
    {
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.subject(null);
        eventBuilder.details(null);
        eventBuilder.build();
    }

    @Test
    public void shouldBeValidWithNoTags()
    {
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.tags(Collections.emptySet());
        eventBuilder.build();
    }

    @Test
    public void shouldRejectNullName()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("AuditEvent.name field is required!");
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.name(null);
        eventBuilder.build();
    }

    @Test
    public void shouldRejectNullType()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("AuditEvent.type field is required!");
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.type(null);
        eventBuilder.build();
    }

    @Test
    public void shouldRejectNullTimestamp()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("AuditEvent.timestamp field is required!");
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.timestamp(null);
        eventBuilder.build();
    }

    @Test
    public void shouldRejectNullInitiator()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("AuditEvent.initiator field is required!");
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.initiator(null);
        eventBuilder.build();
    }

    @Test
    public void shouldRejectNullAction()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("AuditEvent.action field is required!");
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.action(null);
        eventBuilder.build();
    }

    @Test
    public void shouldRejectNullTags()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("AuditEvent.tags field is required!");
        AuditEventBuilder eventBuilder = getEventBuilder();
        eventBuilder.tags((Set<String>)null);
        eventBuilder.build();
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
