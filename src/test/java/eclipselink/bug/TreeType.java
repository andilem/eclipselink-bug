package eclipselink.bug;

import javax.persistence.Cacheable;
import javax.persistence.Entity;

@Entity
@Cacheable(false)
public class TreeType extends DataObject {
    String name;
}
