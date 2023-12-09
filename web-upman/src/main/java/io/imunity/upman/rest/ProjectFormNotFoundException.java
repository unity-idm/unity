package io.imunity.upman.rest;

import jakarta.ws.rs.NotFoundException;

class ProjectFormNotFoundException extends NotFoundException

{
	ProjectFormNotFoundException(String msg)
	{
		super(msg);
	}
}
