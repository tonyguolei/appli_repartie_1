/**
 * Created by tonyguolei on 10/30/2014.
 */

import java.io.*;
import java.util.Properties;

/**
 * lire le fichier .properties
 */
public class ConfigurationFileProperties {
    private Properties propertie;
    //private FileInputStream inputFile;
    private InputStream inputFile;
    /**
     * initialise ConfigurationFileProperties
     */
    public ConfigurationFileProperties() {
        propertie = new Properties();
    }

    /**
     * initialise ConfigurationFileProperties
     *
     * @param filePath
     */
    public ConfigurationFileProperties(String filePath) {
        propertie = new Properties();
        try {
            inputFile = getClass().getResourceAsStream(filePath);
            propertie.load(inputFile);
            inputFile.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Failed to read file");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Failed to read file");
            ex.printStackTrace();
        }
    }

    /**
     * @param key
     * @return key
     */
    public String getValue(String key) {
        if (propertie.containsKey(key)) {
            String value = propertie.getProperty(key);
            return value;
        } else
            return "";
    }


    /**
     * efface le ficher
     */
    public void clear() {
        propertie.clear();
    }//end clear();

    /**
     * @param key
     * @param value
     */
    public void setValue(String key, String value) {
        propertie.setProperty(key, value);
    }
}
