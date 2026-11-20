package io.imunity.rest.admin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AdminJsonBody
{
	Failure value();

	enum Failure
	{
		POLICY, FORM
	}
}
