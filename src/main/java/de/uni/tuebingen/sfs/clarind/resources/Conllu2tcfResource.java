package de.uni.tuebingen.sfs.clarind.resources;

import de.uni.tuebingen.sfs.clarind.Conllu2tcfConfiguration;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import de.uni.tuebingen.sfs.clarind.core.Conllu2tcfConverter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;


@Path("annotate")
public class Conllu2tcfResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Conllu2tcfResource.class);
    public static final String TEXT_TCF_XML = "text/tcf+xml";
    public static final String CONLL = "application/conllu";

    private static final String FALL_BACK_MESSAGE = "Data processing failed";
    private static final String TEMP_FILE_PREFIX = "references-output-temp";
    private static final String TEMP_FILE_SUFFIX = ".xml";

    public Conllu2tcfResource(Conllu2tcfConfiguration config) {
    }

    @Path("convert/bytes")
    @POST
    @Consumes(Conllu2tcfResource.CONLL)
    @Produces(Conllu2tcfResource.TEXT_TCF_XML)
    public Response processConll2tcfWithBytesArray(final InputStream input, @QueryParam("language") String lang) {
        return Conllu2tcfResource.processWithBytesArray(input, lang);
    }

    @Path("convert/stream")
    @POST
    @Consumes(Conllu2tcfResource.CONLL)
    @Produces(Conllu2tcfResource.TEXT_TCF_XML)
    public StreamingOutput processConll2tcfWithStreaming(final InputStream input, @QueryParam("language") String lang) {
        return Conllu2tcfResource.processWithStreaming(input, lang);
    }

    public static Response processWithBytesArray(final InputStream input, String lang) {
        // prepare the storage for TCF output
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // process incoming TCF and output resulting TCF with new annotation layer(s) added
        process(input, output, lang);
        // if no exceptions occur to this point, return OK status and TCF output
        // with the added annotation layer(s)
        return Response.ok(output.toByteArray()).build();
    }

    public static StreamingOutput processWithStreaming(final InputStream input, String lang) {
        // prepare temporary file and temporary output stream for writing TCF
        OutputStream tempOutputData = null;
        File tempOutputFile = null;
        try {
            tempOutputFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            tempOutputData = new BufferedOutputStream(new FileOutputStream(tempOutputFile));
        } catch (IOException ex) {
            if (tempOutputFile != null) {
                tempOutputFile.delete();
            }
            throw new WebApplicationException(createResponse(ex, Response.Status.INTERNAL_SERVER_ERROR));
        }

        // process incoming TCF and output resulting TCF with new annotation layer(s) added
        process(input, tempOutputData, lang);

        // if there were no errors reading and writing TCF data, the resulting
        // TCF can be sent as StreamingOutput from the TCF output temporary file
        return new StreamingTempFileOutput(tempOutputFile);

    }

    private static void process(final InputStream input, OutputStream output, String lang) {
        try {
            // create TextCorpus object from the client request input,
            // only required annotation layers will be read into the object
            Conllu2tcfConverter converter = new Conllu2tcfConverter(lang);
            TextCorpusStored textCorpus = converter.convert(input, output);
            // write TCF to file
            WLData wlData = new WLData(textCorpus);
            WLDObjector.write(wlData, output);

        } catch (WLFormatException ex) {
            throw new WebApplicationException(createResponse(ex, Response.Status.BAD_REQUEST));
        } catch (Exception ex) {
            throw new WebApplicationException(createResponse(ex, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            try {
                if (output != null) {
                    // it's important to close the TextCorpusStreamed, otherwise
                    // the TCF XML output will not be written to the end
                    output.close();
                }
            } catch (Exception ex) {
                throw new WebApplicationException(createResponse(ex, Response.Status.INTERNAL_SERVER_ERROR));
            }
        }
    }

    /* if exception message is provided, use it as it is;
     * if exception message is null, use fall back message
     * (needs to be non-empty String in order to prevent
     * HTTP container generated html message) */
    private static Response createResponse(Exception ex, Response.Status status) {
        String message = ex.getMessage();
        if (message == null) {
            message = FALL_BACK_MESSAGE;
        }
        LOGGER.error("Failed {}", message, ex);
        return Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build();
    }

}
