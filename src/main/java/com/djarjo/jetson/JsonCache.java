package com.djarjo.jetson;

import com.djarjo.common.ReflectionHelper;
import com.djarjo.jetson.converter.JsonConverter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches classes with their JSON accessors
 */
class JsonCache {
    private final static Map<Class<?>, List<JsonAccessor>> _cache = new HashMap<>();

    /**
     * Find JSON accessor in class by its JSON {@code name}
     * <p>
     * Algorithm:
     *   <ul><li>checks name directly</li>
     *   <li>checks annotation attribute {@literal @Json( name = ...)}</li>
     *   <li>tries all {@link ReflectionHelper#GETTER_PREFIXES}</li>
     *   <li>tries all {@link ReflectionHelper#SETTER_PREFIXES}</li>
     *   </ul>
     * If {@code name} cannot be found then both getter and setter prefixes will be tried.
     * </p>
     *
     * @param clazz the class
     * @param name  the JSON name
     * @return accessor or {@code null}
     */
    public static JsonAccessor findAccessorByName(Class<?> clazz, String name) {
        List<JsonAccessor> accessors = getAccessors(clazz);
        if (accessors == null || accessors.isEmpty()) return null;
        if (name == null || name.isBlank()) return null;
        String suffix = Character.toUpperCase(name.charAt(0)) + (name.length() > 1
                ? name.substring(1) : "");

        return accessors.stream().filter(accessor -> {
                    String accName = accessor.getJsonName();
                    if (accName.equals(name)) return true;

                    //--- check getter prefixes
                    for (String prefix : ReflectionHelper.GETTER_PREFIXES) {
                        if (accName.equals(prefix + suffix)) return true;
                    }

                    //--- check setter prefixes
                    for (String prefix : ReflectionHelper.SETTER_PREFIXES) {
                        if (accName.equals(prefix + suffix)) return true;
                    }
                    return false;
                }
        ).findFirst().orElse(null);
    }

    /**
     * Gets all JSON accessors for given class.
     *
     * @param clazz class with JSON annotations
     * @return List of accessors (could be empty)
     */
    public static List<JsonAccessor> getAccessors(Class<?> clazz) {
        List<JsonAccessor> accessors = _cache.get(clazz);
        if (accessors == null) {
            accessors = obtainAllJsonAccessors(clazz);
            _cache.put(clazz, accessors);
        }
        return _cache.get(clazz);
    }

    public static void put(Class<?> clazz, List<JsonAccessor> accessors) {
        _cache.put(clazz, accessors);
    }

    public static List<JsonAccessor> obtainAllJsonAccessors(Class<?> clazz) {
        boolean accessFields = false;
        boolean allMembers = false;
        List<JsonAccessor> accessors = _cache.get(clazz);
        if (accessors != null) return accessors;

        //--- Not yet in cache => obtain all accessors
        accessors = new ArrayList<>();
        Json jsonAnnoAtClass = clazz.getAnnotation(Json.class);

        //--- Class level annotation
        if (jsonAnnoAtClass != null) {
            //--- has converter or toJson() => done
            if (!jsonAnnoAtClass.converter().equals(JsonConverter.class) ||
                    (ReflectionHelper.findMethod(clazz, JsonEncoder.METHOD_TO_JSON) != null)) {
                accessors.add(new JsonAccessor(clazz,
                        JsonConfig.fromAnnotation(jsonAnnoAtClass), null, null, null));
                _cache.put(clazz, accessors);
                return accessors;
            }
            //--- need to scan all fields or methods
            allMembers = true;
            if (jsonAnnoAtClass.accessType().equals(Json.AccessType.FIELD)) {
                accessFields = true;
            }
        }

        //--- Scan fields by traversing the object graph
        Class<?> currentClass = clazz;
        while (currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                JsonAccessor accessor = _createAccessor(clazz, jsonAnnoAtClass, field,
                        allMembers, accessFields);
                if (accessor != null) {
                    accessors.add(accessor);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        //--- Scan methods
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            JsonAccessor accessor = _createAccessor(clazz, jsonAnnoAtClass, method,
                    allMembers, accessFields);
            if (accessor != null) {
                accessors.add(accessor);
            }
        }

        _cache.put(clazz, accessors);
        return accessors;
    }

    private static JsonAccessor _createAccessor(Class<?> clazz, Json jsonAnnoAtClass,
                                                Field field, boolean allMembers, boolean accessFields) {
        JsonConfig config;
        Json annoJson = field.getAnnotation(Json.class);
        if (annoJson != null) {
            config = JsonConfig.fromAnnotation(annoJson);
        } else if (field.isAnnotationPresent(JsonTransient.class)) {
            return null;
        } else if (allMembers && accessFields) {
            config = (jsonAnnoAtClass == null) ? JsonConfig.DEFAULT :
                    JsonConfig.fromAnnotation(jsonAnnoAtClass);
        } else return null;
        return new JsonAccessor(clazz, config, field, null, null);
    }

    private static JsonAccessor _createAccessor(Class<?> clazz, Json jsonAnnoAtClass,
                                                Method method, boolean allMembers, boolean accessFields) {
        JsonConfig config;
        Json annoJson = method.getAnnotation(Json.class);
        if (annoJson != null) {
            config = JsonConfig.fromAnnotation(annoJson);
        } else if (method.isAnnotationPresent(JsonTransient.class)) {
            return null;
        } else if (allMembers && !accessFields && ReflectionHelper.isGetter(method) &&
                !clazz.equals(Object.class)) {
            config = (jsonAnnoAtClass == null) ? JsonConfig.DEFAULT :
                    JsonConfig.fromAnnotation(jsonAnnoAtClass);
        } else return null;
        Method setter = ReflectionHelper.obtainSetterFromMethod(clazz, method);
        return new JsonAccessor(clazz, config, null, method, setter);
    }
}
