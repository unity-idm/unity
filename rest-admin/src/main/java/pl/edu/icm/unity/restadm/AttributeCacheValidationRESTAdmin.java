/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import io.imunity.rest.api.RestAttributeCacheValidationReport;
import io.imunity.rest.mappers.AttributeCacheValidationReportMapper;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Self-validation of the persisted, materialized attributes cache against freshly computed effective
 * attributes.
 */
@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpoint.V1_PATH)
@PrototypeComponent
public class AttributeCacheValidationRESTAdmin implements RESTAdminHandler
{
	private final AttributeCacheValidationService validationService;

	@Autowired
	AttributeCacheValidationRESTAdmin(AttributeCacheValidationService validationService)
	{
		this.validationService = validationService;
	}

	@GET
	@Path("/attribute-cache-validation")
	public String validate(@QueryParam("group") String group) throws EngineException, JsonProcessingException
	{
		if (group != null && !group.startsWith("/"))
			group = "/" + group;
		AttributeCacheValidationReport report = validationService.validate(Optional.ofNullable(group));
		RestAttributeCacheValidationReport restReport = AttributeCacheValidationReportMapper.map(report);
		return Constants.MAPPER.writeValueAsString(restReport);
	}
}
