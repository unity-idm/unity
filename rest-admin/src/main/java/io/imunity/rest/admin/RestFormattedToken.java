package io.imunity.rest.admin;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Date;

public record RestFormattedToken(String type, String value, Long owner, Date created, Date expires, JsonNode contents)
{
}
