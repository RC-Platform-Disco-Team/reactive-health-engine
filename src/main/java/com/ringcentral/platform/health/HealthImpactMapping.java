package com.ringcentral.platform.health;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class HealthImpactMapping {

    private Map<HealthStateEnum, HealthStateEnum> map;

    public static final HealthImpactMapping SUPPRESS_WARNINGS_MAPPING = new HealthImpactMapping(
            new EnumMap<HealthStateEnum, HealthStateEnum>(HealthStateEnum.class) {{
                put(HealthStateEnum.Warning, HealthStateEnum.OK);
            }}
    );

    public static final HealthImpactMapping SUPPRESS_CRITICAL_MAPPING = new HealthImpactMapping(
            new EnumMap<HealthStateEnum, HealthStateEnum>(HealthStateEnum.class) {{
                put(HealthStateEnum.Critical, HealthStateEnum.Warning);
            }}
    );

    public static final HealthImpactMapping SUPPRESS_NON_OK = new HealthImpactMapping(
            new EnumMap<HealthStateEnum, HealthStateEnum>(HealthStateEnum.class) {{
                put(HealthStateEnum.Critical, HealthStateEnum.Warning);
                put(HealthStateEnum.Warning, HealthStateEnum.OK);
            }}
    );

    public static final HealthImpactMapping DEFAULT_IMPACT_MAPPING = new HealthImpactMapping(Collections.emptyMap());

    private HealthImpactMapping(Map<HealthStateEnum, HealthStateEnum> map) {
        this.map =  Collections.unmodifiableMap(map);
    }

    public HealthStateEnum getOrDefault(HealthStateEnum key, HealthStateEnum defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }
}
