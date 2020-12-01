package eclipselink.bug;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Tree extends DataObject {
    int height = 1;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TYPE_ID")
    public TreeType treeType;
    
    @Override
    public String toString() {
        return "Tree [height=" + height + "]";
    }
}
