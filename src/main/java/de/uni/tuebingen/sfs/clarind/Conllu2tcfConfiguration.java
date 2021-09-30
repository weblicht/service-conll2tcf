package de.uni.tuebingen.sfs.clarind;

import io.dropwizard.Configuration;

public class Conllu2tcfConfiguration extends Configuration {
    String language;

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "Conllu2tcfConfiguration(language='" + language + "'')";
    }
}
