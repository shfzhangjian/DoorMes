package cn.iocoder.yudao.module.mes.framework.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesRequestBodyDateTimeContractTest {

    private static final String CONTROLLER_BASE_PACKAGE =
            "cn.iocoder.yudao.module.mes.controller.admin";

    @Test
    void requestBodyLocalDateTimeFieldsMustUseMesDeserializer() throws ClassNotFoundException {
        List<String> violations = new ArrayList<>();
        Set<Class<?>> inspectedTypes = new HashSet<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        for (var candidate : scanner.findCandidateComponents(CONTROLLER_BASE_PACKAGE)) {
            Class<?> controllerClass = Class.forName(candidate.getBeanClassName());
            for (Method method : controllerClass.getDeclaredMethods()) {
                Parameter[] parameters = method.getParameters();
                Type[] genericParameterTypes = method.getGenericParameterTypes();
                for (int index = 0; index < parameters.length; index++) {
                    if (parameters[index].getAnnotation(RequestBody.class) == null) {
                        continue;
                    }
                    inspectType(genericParameterTypes[index], controllerClass.getSimpleName()
                                    + "." + method.getName(), inspectedTypes, violations);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "以下 @RequestBody LocalDateTime 字段未使用 "
                + "MesLocalDateTimeDeserializer:\n" + String.join("\n", violations));
    }

    private void inspectType(Type type,
                             String path,
                             Set<Class<?>> inspectedTypes,
                             List<String> violations) {
        if (type instanceof ParameterizedType parameterizedType) {
            for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                inspectType(actualTypeArgument, path, inspectedTypes, violations);
            }
            return;
        }
        if (type instanceof GenericArrayType genericArrayType) {
            inspectType(genericArrayType.getGenericComponentType(), path, inspectedTypes, violations);
            return;
        }
        if (!(type instanceof Class<?> typeClass)) {
            return;
        }
        if (typeClass.isArray()) {
            inspectType(typeClass.getComponentType(), path, inspectedTypes, violations);
            return;
        }
        if (!isMesRequestType(typeClass) || !inspectedTypes.add(typeClass)) {
            return;
        }

        for (Class<?> current = typeClass; isMesRequestType(current); current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                String fieldPath = current.getSimpleName() + "." + field.getName();
                if (field.getType() == LocalDateTime.class) {
                    JsonDeserialize annotation = field.getAnnotation(JsonDeserialize.class);
                    if (annotation == null || annotation.using() != MesLocalDateTimeDeserializer.class) {
                        violations.add(fieldPath + " (入口: " + path + ")");
                    }
                    continue;
                }
                inspectType(field.getGenericType(), fieldPath, inspectedTypes, violations);
            }
        }
    }

    private boolean isMesRequestType(Class<?> type) {
        Package typePackage = type == null ? null : type.getPackage();
        return typePackage != null && typePackage.getName().startsWith(CONTROLLER_BASE_PACKAGE);
    }

}
