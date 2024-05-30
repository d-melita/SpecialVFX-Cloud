package pt.ulisboa.tecnico.cnv.common;

import java.net.URI;
import java.io.InputStream;

public interface Handler {
    public String actuallyHandle(URI uri, InputStream stream);
}
