package yauhenipo.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public class PropertyReader {

    private Properties prop = new Properties();

    public PropertyReader() {
        try (InputStream is  = this.getClass().getClassLoader().getResourceAsStream("failMarkers.properties")) {
            prop.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<Object> getAllKeys() {
        return prop.keySet();
    }

    public String getPropertyValue(String key) {
        return this.prop.getProperty(key);
    }
}
