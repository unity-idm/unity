import com.google.common.collect.Lists
import pl.edu.icm.unity.engine.api.authn.InvocationContext
import pl.edu.icm.unity.engine.api.authn.LoginSession
import pl.edu.icm.unity.exceptions.EngineException
import pl.edu.icm.unity.types.registration.EnquiryResponse
import pl.edu.icm.unity.types.registration.EnquiryResponseBuilder
import pl.edu.icm.unity.types.registration.RegistrationContext
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode
import pl.edu.icm.unity.types.registration.RegistrationRequest
import pl.edu.icm.unity.types.registration.RegistrationRequestBuilder

for(int i=0; i<12; i++)
	addRequest(i);
for(int i=0; i<13000; i++)
	addEnquiry(i);

void addRequest(int i)
{
	try
	{
		RegistrationRequest request = new RegistrationRequestBuilder()
			.withFormId("test")
			.withAddedIdentity("userName", "test_" + i).endIdentity()
			.build();
		RegistrationContext context = new RegistrationContext(false, TriggeringMode.manualStandalone);
		
		registrationsManagement.submitRegistrationRequest(request, context);
	} catch (EngineException e)
	{
		throw new IllegalStateException("Failed to add a request", e);
	}
}

void addEnquiry(int i)
{
	try
	{
		EnquiryResponse request = new EnquiryResponseBuilder()
				.withFormId("testE")
				.withAddedIdentity("userName", "testE_" + i).endIdentity()
				.build();
		RegistrationContext context = new RegistrationContext(false, TriggeringMode.manualStandalone);
		InvocationContext ctx = new InvocationContext(null, null, null);
		LoginSession loginSession = new LoginSession();
		loginSession.setEntityId(i+10);
		ctx.setLoginSession(loginSession);
		InvocationContext.setCurrent(ctx);
		enquiryManagement.submitEnquiryResponse(request, context);
	} catch (EngineException e)
	{
		throw new IllegalStateException("Failed to add a response", e);
	}
}