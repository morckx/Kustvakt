package de.ids_mannheim.korap.constant;

/** Defines some predefined roles used in the system.
 * 
 * @author margaretha
 *
 */
public enum PredefinedRole {
    USER_GROUP_ADMIN(1), USER_GROUP_MEMBER(2), QUERY_ACCESS_ADMIN(3), QUERY_ACCESS_MEMBER(4);
    
    private int id;
    private String name;

    PredefinedRole (int i) {
        this.id = i;
        this.name = name().toLowerCase().replace("_", " "); 
    }
    
    public int getId () {
        return id;
    }
    
    @Override
    public String toString () {
        return this.name;
    }
}
