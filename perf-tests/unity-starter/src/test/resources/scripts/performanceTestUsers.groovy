/*
 * Creates many users
 *
 * Depends on defaultContentInitializer.groovy
 */
import groovy.transform.Field
import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.types.basic.Attribute
import pl.edu.icm.unity.types.basic.AttributeType
import pl.edu.icm.unity.types.basic.EntityParam
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.Identity
import pl.edu.icm.unity.types.basic.IdentityParam
import pl.edu.icm.unity.types.basic.VerifiableEmail
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo


@Field final String NAME_ATTR = "name"
@Field final String EMAIL_ATTR = "email";
@Field final int ENTITIES = 50;

log.info("Creating demo content...");

try
{
	Map<String, AttributeType> existingATs = attributeTypeManagement.getAttributeTypesAsMap();
	if (!existingATs.containsKey(NAME_ATTR) || !existingATs.containsKey(EMAIL_ATTR))
	{
		log.error("Demo contents can be only installed if standard types were installed " +  
			"prior to it. Attribute types cn, o and email are required.");
		return;
	}
	
	for (int i=1; i<ENTITIES; i++)
		createExampleUser(i);
	
} catch (Exception e)
{
	log.warn("Error loading demo contents. This can happen and by far is not critical. " +
			"It means that demonstration contents was not loaded to your database, " +
			"usaully due to conflict with its existing data", e);
}



void createExampleUser(int suffix)
{
	IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "perf-user-" + suffix);
	Identity base = entityManagement.addEntity(toAdd, EntityState.valid, false);


	EntityParam entityP = new EntityParam(base.getEntityId());


	VerifiableEmail emailVal = new VerifiableEmail("per" + suffix + "@example.com", new ConfirmationInfo(true));
	emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
	emailVal.getConfirmationInfo().setConfirmed(true);
	Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", emailVal);
	attributesManagement.createAttribute(entityP, emailA);

	Attribute cnA = StringAttribute.of(NAME_ATTR, "/", "Perf user " + suffix);
	attributesManagement.createAttribute(entityP, cnA);

	PasswordToken pToken = new PasswordToken("the!test12");
	entityCredentialManagement.setEntityCredential(entityP, EngineInitialization.DEFAULT_CREDENTIAL, pToken.toJson());
	log.warn("Demo user 'demo-user-" + suffix + "' was created");
}

