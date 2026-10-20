package my.xtream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

    public class YAML {
        private Map<String, Object> _yaml;

        YAML(String file) {
            try (InputStream in = Files.newInputStream(Path.of(file))) {
                org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
                _yaml = yaml.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getString(String me) {
            return null;
        }
        
        boolean has(String key) {
            return _yaml.containsKey(key);
        }
        
        Map<String,Object> get(String key) {
            return (Map<String,Object>) _yaml.get(key);
        }

       public boolean getBoolean(String key) {
            return (boolean) _yaml.get(key);
        }
    }