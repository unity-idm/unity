import com.google.common.collect.Lists

import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.exceptions.EngineException
import pl.edu.icm.unity.types.registration.RegistrationContext
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode
import pl.edu.icm.unity.types.registration.RegistrationForm
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder
import pl.edu.icm.unity.types.registration.RegistrationRequest
import pl.edu.icm.unity.types.registration.RegistrationRequestBuilder

for(int i=0; i<10000; i++)
	addRequest();

void addRequest()
{
	try
	{
		RegistrationRequest request = new RegistrationRequestBuilder()
			.withFormId("test")
			.withAddedIdentity("userName", "test").endIdentity()
			.build();
		RegistrationContext context = new RegistrationContext(false, TriggeringMode.manualAdmin);
		
		registrationsManagement.submitRegistrationRequest(request, context);
	} catch (EngineException e)
	{
		throw new IllegalStateException("Failed to add a request");
	}
}