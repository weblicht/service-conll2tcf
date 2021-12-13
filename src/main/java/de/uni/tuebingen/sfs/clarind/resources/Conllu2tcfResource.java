package de.uni.tuebingen.sfs.clarind.resources;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import de.uni.tuebingen.sfs.clarind.Conllu2tcfConfiguration;


@Path("annotate")
public class Conllu2tcfResource {

    public Conllu2tcfResource(Conllu2tcfConfiguration config) {
    }

    @Path("convert/bytes")
    @POST
    @Consumes(Conllu2tcfProcessing.CONLL)
    @Produces(Conllu2tcfProcessing.TEXT_TCF_XML)
    public Response processConll2tcfWithBytesArray(final InputStream input, @QueryParam("language") String lang) {
        return Conllu2tcfProcessing.processWithBytesArray(input, lang);
    }

    @Path("convert/stream")
    @POST
    @Consumes(Conllu2tcfProcessing.CONLL)
    @Produces(Conllu2tcfProcessing.TEXT_TCF_XML)
    public StreamingOutput processConll2tcfWithStreaming(final InputStream input, @QueryParam("language") String lang) {
        return Conllu2tcfProcessing.processWithStreaming(input, lang);
    }

}
