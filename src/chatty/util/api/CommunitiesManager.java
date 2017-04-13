
package chatty.util.api;

import chatty.util.JSONUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class CommunitiesManager {
    
    private static final Logger LOGGER = Logger.getLogger(CommunitiesManager.class.getName());
    
    private final TwitchApi api;
    
    private final Set<Community> withInfo = new HashSet<>();
    private final Set<Community> all = new HashSet<>();
    
    private final Set<String> error = Collections.synchronizedSet(new HashSet<>());
    
    public CommunitiesManager(TwitchApi api) {
        this.api = api;
    }
    
    public synchronized Community getCachedByIdWithInfo(String id) {
        for (Community c : withInfo) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }
    
    public synchronized Community getCachedById(String id) {
        for (Community c : all) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }
    
    public synchronized void getById(String id, CommunityListener listener) {
        Community c = getCachedById(id);
        if (c != null) {
            listener.received(c, null);
        } else {
            // Not cached, request
            if (!error.contains(id)) {
                api.requests.getCommunityById(id, (r, e) -> {
                    if (r == null) {
                        error.add(id);
                    }
                    listener.received(r, e);
                });
            }
        }
    }
    
    /**
     * Add the given Community to the cache. Both objects with info and without
     * are accepted.
     * 
     * @param c 
     */
    public synchronized void addCommunity(Community c) {
        if (c != null) {
            all.add(c);
            if (c.getSummary() != null) {
                withInfo.add(c);
            }
        }
    }
    
    public static class Community implements Comparable<Community> {
        
        public static final Community EMPTY = new Community(null, "");
        
        private final String name;
        private final String id;
        private final String summary;
        private final String rules;
        
        public Community(String id, String name, String summary, String rules) {
            this.name = name;
            this.id = id;
            this.summary = summary;
            this.rules = rules;
        }
        
        public Community(String id, String name) {
            this(id, name, null, null);
        }
        
        public String getName() {
            return name;
        }
        
        public String getId() {
            return id;
        }
        
        /**
         * The summary in HTML format.
         * 
         * @return The summary, or null if not set
         */
        public String getSummary() {
            return summary;
        }
        
        /**
         * The rules in HTML format.
         * 
         * @return The rules, or null if not set
         */
        public String getRules() {
            return rules;
        }
        
        /**
         * Check if this is a valid community with at least an id and name.
         * 
         * @return 
         */
        public boolean isValid() {
            return id == null || name == null || name.isEmpty();
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Community other = (Community) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public int compareTo(Community o) {
            if (o != null && Objects.equals(id, o.id)) {
                return 0;
            }
            if (o == null || o.name == null) {
                return -1;
            }
            if (name == null) {
                return 1;
            }
            return name.compareToIgnoreCase(o.name);
        }

    }
    
    public interface CommunityListener {
        
        /**
         * 
         * @param community The community, or null if an error occured
         * @param error A description of the error, or null if none is specified
         */
        public void received(Community community, String error);
    }
    
    public interface CommunityTopListener {
        public void received(Collection<Community> communities);
    }
    
    public interface CommunityPutListener {
        public void result(String error);
    }
    
    public static Collection<Community> parseTop(String text) {
        Collection<Community> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(text);
            JSONArray communities = (JSONArray) root.get("communities");
            
            
            for (Object o : communities) {
                JSONObject community = (JSONObject)o;
                String id = JSONUtil.getString(community, "_id");
                String name = JSONUtil.getString(community, "name");
                if (id != null && name != null) {
                    result.add(new Community(id, name));
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing Top Communities: "+ex);
        }
        return result;
    }
    
    public static Community parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject community = (JSONObject) parser.parse(text);
            String id = JSONUtil.getString(community, "_id");
            String name = JSONUtil.getString(community, "name");
            String summary = JSONUtil.getString(community, "description_html");
            String rules = JSONUtil.getString(community, "rules_html");
            if (id != null && name != null) {
                return new Community(id, name, summary, rules);
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing Community: "+ex);
        }
        return null;
    }
    
}
