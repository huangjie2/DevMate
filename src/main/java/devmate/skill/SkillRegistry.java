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
 * Skill 注册中心
 * 
 * 负责管理所有 Skill 的注册、发现和调用
 */
@ApplicationScoped
public class SkillRegistry {

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();

    /**
     * 注册 Skill
     * 
     * @param skill Skill 实例
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
     * 注销 Skill
     * 
     * @param name Skill 名称
     * @return 是否注销成功
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
     * 查找 Skill
     * 
     * @param name Skill 名称
     * @return Skill 实例（可选）
     */
    public Optional<Skill> find(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    /**
     * 检查 Skill 是否存在
     * 
     * @param name Skill 名称
     * @return 是否存在
     */
    public boolean exists(String name) {
        return skills.containsKey(name);
    }

    /**
     * 获取所有 Skill
     * 
     * @return Skill 列表
     */
    public List<Skill> allSkills() {
        return List.copyOf(skills.values());
    }

    /**
     * 获取所有启用的 Skill
     * 
     * @return 启用的 Skill 列表
     */
    public List<Skill> enabledSkills() {
        return skills.values().stream()
            .filter(Skill::isEnabled)
            .collect(Collectors.toList());
    }

    /**
     * 按类别获取 Skill
     * 
     * @param category 类别名称
     * @return Skill 列表
     */
    public List<Skill> byCategory(String category) {
        return skills.values().stream()
            .filter(skill -> category.equals(skill.category()))
            .collect(Collectors.toList());
    }

    /**
     * 获取 Skill 数量
     * 
     * @return Skill 数量
     */
    public int size() {
        return skills.size();
    }

    /**
     * 清空所有 Skill
     */
    public void clear() {
        skills.clear();
        Log.info("All skills have been cleared");
    }

    /**
     * 生成给 LLM 的工具列表描述
     * 
     * @return 格式化的工具描述
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
     * 生成 OpenAI Function Calling 格式的工具定义
     * 
     * @return OpenAI 工具定义列表
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
