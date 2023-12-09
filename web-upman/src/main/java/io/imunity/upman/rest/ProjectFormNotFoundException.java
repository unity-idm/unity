package io.imunity.upman.rest;

import javax.ws.rs.NotFoundException;

class ProjectFormNotFoundException extends NotFoundException

{
	ProjectFormNotFoundException(String msg)
	{
		super(msg);
	}
}
