package trainsys.dao;

import trainsys.dao.entity.StationEntity;
import trainsys.dao.mapper.StationMapper;
import trainsys.util.Types.StationID;
import trainsys.util.Types.StationName;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
/**
 * 站点持久化管理器。
 * 负责站点基础数据的加载，以及站点名称和站点 ID 的双向映射。
 */
public class StationManager {
    private final Map<Integer, String> idToName;
    private final Map<String, Integer> nameToID;
    private final StationMapper stationMapper;

    public StationManager(StationMapper stationMapper) {
        this.stationMapper = stationMapper;
        this.idToName = new HashMap<>();
        this.nameToID = new HashMap<>();
        loadStationsFromDB();
        if (idToName.isEmpty()) {
            log.info("No station data found in database, loading from data/station.txt");
            loadStationsFromFile();
        }
    }

    /**
     * 从数据库加载站点信息到内存索引。
     */
    private void loadStationsFromDB() {
        List<StationEntity> stations = stationMapper.selectList(new QueryWrapper<StationEntity>().orderByAsc("id"));
        for (StationEntity station : stations) {
            idToName.put(station.getId(), station.getName());
            nameToID.put(station.getName(), station.getId());
        }
    }

    /**
     * 根据站点 ID 获取站点名称对象。
     */
    public StationName getStationName(StationID stationID) {
        String name = idToName.get(stationID.value());
        return new StationName(name != null ? name : "");
    }

    /**
     * 根据站点名称获取站点 ID 对象。
     */
    public StationID getStationID(String stationName) {
        Integer id = nameToID.get(stationName);
        return new StationID(id != null ? id : -1);
    }

    /**
     * 根据站点名称查询原始整型 ID。
     */
    public Integer nameToID(String stationName) {
        return nameToID.get(stationName);
    }

    /**
     * 根据整型 ID 查询站点名称。
     */
    public String idToName(int stationId) {
        return idToName.get(stationId);
    }

    /**
     * 获取全部站点名称。
     */
    public List<String> getAllStationNames() {
        return new ArrayList<>(nameToID.keySet());
    }

    /**
     * 当数据库为空时，从本地文件加载站点并回填数据库。
     */
    private void loadStationsFromFile() {
        try {
            Path path = Path.of("data", "station.txt");
            if (!Files.exists(path)) {
                log.warn("Station file data/station.txt does not exist");
                return;
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int count = 0;
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+", 2);
                if (parts.length < 2) {
                    continue;
                }

                int id = Integer.parseInt(parts[0]);
                String name = parts[1];

                StationEntity entity = new StationEntity();
                entity.setId(id);
                entity.setName(name);
                if (stationMapper.selectById(id) == null) {
                    stationMapper.insert(entity);
                } else {
                    stationMapper.updateById(entity);
                }

                idToName.put(id, name);
                nameToID.put(name, id);
                count++;
            }
            log.info("Loaded {} stations from file into database", count);
        } catch (Exception e) {
            log.error("Failed to load station data from file", e);
        }
    }
}
