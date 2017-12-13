package de.ids_mannheim.korap.constant;

public enum VirtualCorpusType {

    PREDEFINED, PROJECT, PRIVATE, PUBLISHED;
    
    public String displayName () {
        return name().toLowerCase();

    }
}
