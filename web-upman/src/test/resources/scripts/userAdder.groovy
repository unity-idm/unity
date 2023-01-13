import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.IdentityParam


log.info("Context is {}", context);
IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, context);
entityManagement.addEntity(toAdd, EntityState.valid);
