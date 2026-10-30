package com.example.solution.fde;

import java.util.Map;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/** Trusted policy plug-in. SQL scoping and all output paths use this same boundary. */
public interface FdePolicy {
    String id();
    boolean discoverable();
    void lockForWrite();
    Specification<FdeObject> scope();
    Map<String, Object> prepareCreate(Map<String, Object> data);
    void stamp(FdeObject object, Map<String, Object> data);
    String searchText(Map<String, Object> data);
    String businessScope(Map<String, Object> data);
    boolean canAct(FdeObject object, String action);
    Set<String> visibleFields(FdeSchema.ObjectType type, FdeObject object);
}
