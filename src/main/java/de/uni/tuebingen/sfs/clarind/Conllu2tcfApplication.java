package de.uni.tuebingen.sfs.clarind;

import de.uni.tuebingen.sfs.clarind.resources.*;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class Conllu2tcfApplication extends Application<Conllu2tcfConfiguration> {
    public static void main(String[] args) throws Exception {
        new Conllu2tcfApplication().run(args);
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void initialize(Bootstrap<Conllu2tcfConfiguration> bootstrap) {
    }

    @Override
    public void run(Conllu2tcfConfiguration configuration, Environment environment) throws Exception {
        System.out.println("configuration: \n" + configuration);
        Conllu2tcfResource stickerResource = new Conllu2tcfResource(configuration);


        IndexResource indexResource = new IndexResource();
        environment.jersey().register(stickerResource);
        environment.jersey().register(indexResource);
    }
}

