package konopka.util;

import jdk.nashorn.internal.ir.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;


public class SymbolMap {
    private final Properties symbolmap;
    public SymbolMap(File file) {
        symbolmap = new Properties();
        try {
            System.out.println("Reading symbols.xml: " + file.getAbsolutePath());
            //Populate the symbol map from the XML file
            symbolmap.loadFromXML( file.toURI().toURL().openStream() );
            System.out.println(symbolmap.size() + " symbols loaded.");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String lookupSymbol(String symbol, String... variables) {
        //Retrieve the value of the associated key
        String message = symbolmap.getProperty(symbol);
        if(message == null) {
            System.err.println("Symbol " + symbol + " not found.");
            return "";
        }
        //Interpolate parameters if necessary
        //and return the message
        return String.format(message, new Object[]{variables});
    }
}
