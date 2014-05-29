/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * RESTful API implementation.
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
public class RESTAdmin
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTAdmin.class);
	private IdentitiesManagement identitiesMan;
	private GroupsManagement groupsMan;
	private AttributesManagement attributesMan;
	private ObjectMapper mapper = Constants.MAPPER;
	
	public RESTAdmin(IdentitiesManagement identitiesMan, GroupsManagement groupsMan,
			AttributesManagement attributesMan)
	{
		super();
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attributesMan = attributesMan;
	}

	@Path("/entity/{entityId}")
	@GET
	public String getEntity(@PathParam("entityId") long entityId) throws EngineException, JsonProcessingException
	{
		log.debug("getEntity query for " + entityId);
		Entity entity = identitiesMan.getEntity(new EntityParam(entityId));
		return mapper.writeValueAsString(entity);
	}
	
	@Path("/entity/{entityId}/groups")
	@GET
	public String getGroups(@PathParam("entityId") long entityId) throws EngineException, JsonProcessingException
	{
		log.debug("getGroups query for " + entityId);
		Collection<String> groups = identitiesMan.getGroups(new EntityParam(entityId));
		return mapper.writeValueAsString(groups);
	}

	@Path("/entity/{entityId}/attributes")
	@GET
	public String getAttributes(@PathParam("entityId") long entityId,
			@QueryParam("group") String group) throws EngineException, JsonProcessingException
	{
		if (group == null)
			group = "/";
		log.debug("getAttributes query for " + entityId + " in " + group);
		Collection<AttributeExt<?>> attributes = attributesMan.getAttributes(
				new EntityParam(entityId), group, null);
		
		List<AttributeRepresentation> wrapped = new ArrayList<AttributeRepresentation>(attributes.size());
		for (AttributeExt<?> a: attributes)
			wrapped.add(new AttributeRepresentation(a));
		
		return mapper.writeValueAsString(wrapped);
	}

	@Path("/group/{groupPath}")
	@GET
	public String getGroupContents(@PathParam("groupPath") String group) throws EngineException, JsonProcessingException
	{
		log.debug("getGroupContents query for " + group);
		GroupContents contents = groupsMan.getContents(group, GroupContents.GROUPS | GroupContents.MEMBERS);
		return mapper.writeValueAsString(new GroupContentsRepresentation(contents));
	}
}
