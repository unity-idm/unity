/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic.audit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.lang.NonNull;
import pl.edu.icm.unity.Constants;

import java.util.Objects;

/**
 * Holds information entity that initiate event or was subject of event.
 *
 * @author R. Ledzinski
 */
public class AuditEntity
{
    Long entityId;
    String name;
    String email;

    public AuditEntity(@NonNull final Long entityId, @NonNull final String name, @NonNull final String email)
    {
        this.entityId = entityId;
        this.name = name;
        this.email = email;
    }

    private AuditEntity()
    {
    }

    public Long getEntityId()
    {
        return entityId;
    }

    public String getName()
    {
        return name;
    }

    public String getEmail()
    {
        return email;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AuditEntity that = (AuditEntity) o;
        return entityId.equals(that.entityId) &&
                name.equals(that.name) &&
                email.equals(that.email);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(entityId);
    }

    @Override
    public String toString()
    {
        return "AuditEntity{" +
                "entityId=" + entityId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
