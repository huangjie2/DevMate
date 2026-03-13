package devmate.skill;

import com.fasterxml.jackson.databind.JsonNode;
import devmate.util.JsonMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Skill Registry
 * 
 * Manages registration, discovery and invocation of all Skills
 */
@ApplicationScoped
public class SkillRegistry {

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();

    /**
     * Register Skill
     * 
     * @param skill Skill instance
     */
    public void register(Skill skill) {
        String name = skill.name();
        if (skills.containsKey(name)) {
            Log.warnf("Skill '%s' already registered, will be overwritten", name);
        }
        skills.put(name, skill);
        Log.infof("Registered skill: %s - %s", name, skill.description());
    }

    /**
     * Unregister Skill
     * 
     * @param name Skill name
     * @return Whether unregistration was successful
     */
    public boolean unregister(String name) {
        Skill removed = skills.remove(name);
        if (removed != null) {
            Log.infof("Unregistered skill: %s", name);
            return true;
        }
        return false;
    }

    /**
     * Find Skill
     * 
     * @param name Skill name
     * @return Skill instance (optional)
     */
    public Optional<Skill> find(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    /**
     * Check if Skill exists
     * 
     * @param name Skill name
     * @return Whether exists
     */
    public boolean exists(String name) {
        return skills.containsKey(name);
    }

    /**
     * Get all Skills
     * 
     * @return Skill list
     */
    public List<Skill> allSkills() {
        return List.copyOf(skills.values());
    }

    /**
     * Get all enabled Skills
     * 
     * @return Enabled Skill list
     */
    public List<Skill> enabledSkills() {
        return skills.values().stream()
            .filter(Skill::isEnabled)
            .collect(Collectors.toList());
    }

    /**
     * Get Skills by category
     * 
     * @param category Category name
     * @return Skill list
     */
    public List<Skill> byCategory(String category) {
        return skills.values().stream()
            .filter(skill -> category.equals(skill.category()))
            .collect(Collectors.toList());
    }

    /**
     * Get Skill count
     * 
     * @return Skill count
     */
    public int size() {
        return skills.size();
    }

    /**
     * Clear all Skills
     */
    public void clear() {
        skills.clear();
        Log.info("All skills have been cleared");
    }

    /**
     * Generate tool list description for LLM
     * 
     * @return Formatted tool description
     */
    public String toToolsDescription() {
        return skills.values().stream()
            .filter(Skill::isEnabled)
            .map(skill -> String.format(
                """
                ### %s
                %s
                
                **Schema:**
                ```json
                %s
                ```
                """,
                skill.name(),
                skill.description(),
                JsonMapper.toPrettyJson(skill.inputSchema())
            ))
            .collect(Collectors.joining("\n---\n"));
    }

    /**
     * Generate OpenAI Function Calling format tool definitions
     * 
     * @return OpenAI tool definition list
     */
    public List<Map<String, Object>> toOpenAiTools() {
        return skills.values().stream()
            .filter(Skill::isEnabled)
            .map(skill -> Map.<String, Object>of(
                "type", "function",
                "function", Map.of(
                    "name", skill.name(),
                    "description", skill.description(),
                    "parameters", jsonNodeToMap(skill.inputSchema())
                )
            ))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        return JsonMapper.fromJson(node.toString(), Map.class);
    }
}