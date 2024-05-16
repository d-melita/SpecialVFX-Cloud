package pt.ulisboa.tecnico.cnv.middleware;

import java.util.ArrayList;
import java.util.List;

public class Request {

    private enum Endpoint {

        BLUR("blurimage"),
        ENHANCE("enhanceimage"),
        RAYTRACER("raytracer");

        private String endpoint;

        Endpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return this.endpoint;
        }
    }

    private Endpoint endpoint;

    private List<String> arguments;

    /*
     * Receive a URI (e.g. /raytracer?arg1=val1&arg2=val2&arg3=val3)
     * and parse it into a Request object.
     */
    public Request(String URI) {

        String[] parts = URI.split("/")[0].split("?");

        switch (parts[0]) {
            case "blur":
                this.endpoint = Endpoint.BLUR;
                break;
            case "enhance":
                this.endpoint = Endpoint.ENHANCE;
                break;
            case "raytracer":
                this.endpoint = Endpoint.RAYTRACER;
                break;
            default:
                System.out.println("Unknown request type");
                throw new RuntimeException("tried to create unknow ");
        }

        List<String> arguments = new ArrayList<String>();
        
        if (this.endpoint == Endpoint.RAYTRACER) {
            String[] unparsedArguments = parts[1].split("&");
            for (String part : unparsedArguments) {
                arguments.add(part.split("=")[1]);
            }
            this.arguments = arguments;
        }
    }

    public String getURI() {
        return this.toString();
    }

    @Override
    public String toString() {
        if (this.endpoint == Endpoint.RAYTRACER) {
            return "/" + this.endpoint + "?" + String.join("&", this.arguments);
        }
        return "/" + this.endpoint;
    }
}
