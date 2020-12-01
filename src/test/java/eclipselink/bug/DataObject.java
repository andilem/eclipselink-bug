package eclipselink.bug;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class DataObject {
    @Id
    @Column(insertable = false) // another bug - EL tries to insert 0 if persisted via OneToMany relationship
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    
    @Version
    @Column(name = "VERSION", nullable = false, columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Timestamp version;
    
    @Transient
    public final boolean isTransient() {
        return id == 0;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + id + "]";
    }
}
