package pt.ulisboa.tecnico.cnv.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.Map;
import java.util.Base64;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler;

/**
 * Lambda function to handle raytracer requests.
 */
public class EnhanceLambda implements RequestHandler<Map<String, String>, String> {
    EnhanceImageHandler handler = new EnhanceImageHandler();

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        try {
            URI requestedUri = new URI(event.get("uri"));
            InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(event.get("body")));
            return handler.actuallyHandle(requestedUri, stream);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
