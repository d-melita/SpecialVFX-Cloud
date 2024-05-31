package pt.ulisboa.tecnico.cnv.imageproc;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class ImageProcessingHandler implements HttpHandler, RequestHandler<Map<String,String>, String> {

    abstract BufferedImage process(BufferedImage bi) throws IOException;

    public String actuallyHandle(URI requestedUri, InputStream stream)  {
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        // System.out.printf("String result = %s\n", result);
        String[] resultSplits = result.split(",");
        String format = resultSplits[0].split("/")[1].split(";")[0];

        // Result syntax: data:image/<format>;base64,<encoded image>
        String output = handleRequest(resultSplits[1], format);
        output = String.format("data:image/%s;base64,%s", format, output);

        return output;
    }

    private String handleRequest(String inputEncoded, String format) {
        byte[] decoded = Base64.getDecoder().decode(inputEncoded);
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            BufferedImage bi = ImageIO.read(bais);
            bi = process(bi);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, format, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        // Handling CORS
        try {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
                t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
                t.sendResponseHeaders(204, -1);
                return;
            }

            InputStream stream = t.getRequestBody();

            String output = this.actuallyHandle(null, stream);
            t.sendResponseHeaders(200, output.length());
            OutputStream os = t.getResponseBody();
            os.write(output.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        return handleRequest(event.get("body"), event.get("fileFormat"));
    }
}
