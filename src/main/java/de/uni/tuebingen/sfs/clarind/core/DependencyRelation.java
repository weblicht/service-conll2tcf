package de.uni.tuebingen.sfs.clarind.core;

import eu.clarin.weblicht.wlfxb.tc.api.Token;

public class DependencyRelation {

    private Token token;
    private String relationType;
    private int depID;
    private int govID;
    private int sentenceID;

    public DependencyRelation(Token token, String relationType, int depID, int govID, int sentenceID) {
        this.token = token;
        this.relationType = relationType;
        this.depID = depID;
        this.govID = govID;
        this.sentenceID = sentenceID;
    }

    public Token getToken() {
        return token;
    }

    public String getRelationType() {
        return relationType;
    }

    public int getDepID() {
        return depID;
    }

    public int getGovID() {
        return govID;
    }

    public int getSentenceID() {
        return sentenceID;
    }
}
